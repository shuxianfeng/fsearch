package com.movision.fsearch.plugins;

import java.io.File;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.movision.fsearch.L;
import com.movision.fsearch.core.DataSourceManager;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import com.petkit.base.config.PropertiesConfig;
import com.petkit.base.repository.db.JdbcTemplate;
import com.petkit.base.repository.db.MultiTableMapHandler;
import com.petkit.base.repository.db.StringPropertyHandler;
import com.petkit.base.utils.FileUtil;
import com.petkit.base.utils.FormatUtil;
import com.petkit.base.utils.StringUtil;
import com.movision.fsearch.analysis.TokenUtil;
import com.movision.fsearch.core.Indexer;
import com.movision.fsearch.core.Searcher;
import com.movision.fsearch.core.SearcherOptions;

public class ProductIndexer implements Indexer {

	private Timer timer = null;

	@Override
	public Path fullIndex(Searcher searcher) throws Exception {

        //获取搜索选项
        SearcherOptions options = searcher.getOptions();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        File dir = new File(options.getPath() + "-" + dateFormat.format(new Date()));
        dir.mkdirs();
		Path path = dir.toPath();
        // 获取一个存储在文件系统中的索引的位置
        Directory directory = SimpleFSDirectory.open(path);

		try {
            //批量查询，每次300条数据
            int batch = 300;
			Object lastId = null;
            //实例化JdbcTemplate
            JdbcTemplate template = DataSourceManager.getJdbcTemplate();
			int total = 0;
			while (true) {
				Object[] params = null;
                //构建sql
                //此处是查询商品信息，企业名称，会员身份，会员的省，市，区，商品品牌名称，商品分类名称
                String sql = "select p.*, m.enterpriseName member_enterpriseName, m.identify member_identify, " +
                        "m.province member_province, m.city member_city, b.CNName brand_CNName, c.name scate_name "
                        + " from (select * from t_p_product where status=1) p"
						+ " left outer join t_m_member m on m.id=p.createid"
						+ " left outer join t_p_brand b on b.id=p.brandid"
						+ " left outer join t_p_category c on c.id=p.scateid";
				if (lastId != null) {
					params = new Object[] { lastId };
					sql += " where p.id>?";
				}
                //从小到大排序
                sql += " order by p.id asc";
                //获取数据库查询结果
                List<Map<String, Object>> docs = template.findList(sql, params,
						0, batch, MultiTableMapHandler.CASESENSITIVE);
				if (docs.isEmpty()) {
					break;
				}
                //获取结果中最后一条数据的id
                lastId = docs.get(docs.size() - 1).get("t_p_product.id");
				List<Document> documents = new ArrayList<Document>(docs.size());
                //遍历查询结果
                for (Map<String, Object> docAsMap : docs) {
					Map<String, Object> doc = new HashMap<String, Object>();

					for (Entry<String, Object> field : docAsMap.entrySet()) {
                        //这边生成 key=member_1 的键值对
                        String key = null;
						if (field.getKey().startsWith("t_m_member.")) {
							key = "member_"
									+ field.getKey().substring(
											field.getKey().indexOf('.') + 1);
						}else if (field.getKey().startsWith("t_p_brand.")) {
							key = "brand_"
									+ field.getKey().substring(
											field.getKey().indexOf('.') + 1);
						}else if (field.getKey().startsWith("t_p_category.")) {
							key = "scate_"
									+ field.getKey().substring(
											field.getKey().indexOf('.') + 1);
						}else {
							key = field.getKey().substring(
									field.getKey().indexOf('.') + 1);
						}
						if (field.getValue() == null) {
							continue;
						}
						doc.put(key, field.getValue());
					}

					Map<String, Object> parsedDoc = parseRawDocument(doc);
                    //生成搜索结果文档
                    Document document = searcher.parseDocument(parsedDoc);
					if (L.isInfoEnabled()) {
						L.info(this.getClass() + " saving document: "
								+ document);
					}
					documents.add(document);
				}
                //把搜索结果文档添加到索引中
                IndexWriter writer = new IndexWriter(directory,
						new IndexWriterConfig(Searcher.ANALYZER));
				writer.addDocuments(documents);
				writer.close();
				total += documents.size();
			}
			L.warn(this.getClass() + " saved documents: " + total);
			return path;
		} catch (Exception e) {
			FileUtil.delete(path.toFile());
			L.error("执行异常>>>",e);
			throw e;
		} finally {
			FileUtil.close(directory);
		}
	}

    /**
     * 解析搜索结果的一行数据，再map中封装必要参数：_p， price1， publishTime1， brandid1， scateid1
     *
     * @param docAsMap
     * @return
     * @throws Exception
     */
    @Override
	public Map<String, Object> parseRawDocument(Map<String, Object> docAsMap)
            throws Exception {
        //1 参数
		String paramValues = FormatUtil.parseString(docAsMap.get("paramValues"));
		if (null != paramValues  && !paramValues.isEmpty()){
			paramValues.replaceAll(Searcher.SPLIT, Searcher.TOKEN);
			docAsMap.put("_p", paramValues.toString());
		}
        //2 价格
		{
			Float price = FormatUtil.parseFloat(docAsMap.get("price"));
			if (null == price || price.floatValue() < 0){
				price = Float.MAX_VALUE;
			}
			docAsMap.put("price1", price);
		}
        //发布时间
		{
			String publishtime = FormatUtil.parseString(docAsMap.get("publishTime"));
			if(null != publishtime && publishtime.length() > 0 ){
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Date date = sdf.parse(publishtime);
				publishtime = DateTools.dateToString(date, Resolution.SECOND);
				long lpubtime= Long.valueOf(publishtime);
				docAsMap.put("publishTime1", lpubtime);
			}
		}
        //品牌
		{
			Integer brandid = FormatUtil.parseInteger(docAsMap.get("brandid"));
			if(null != brandid && brandid.intValue() > 0){
				docAsMap.put("brandid1", brandid);
			}
		}
        //分类
		{
			Integer scateid = FormatUtil.parseInteger(docAsMap.get("scateid"));
			if(null != scateid && scateid.intValue() > 0){
				docAsMap.put("scateid1", scateid);
			}
		}
		return docAsMap;
	}

	@Override
	public void init(SearcherOptions options, PropertiesConfig config)
            throws Exception {
        /**
         * 延时0毫秒后重复的执行task(即更新字典)，周期是30分钟
         */
		timer = new Timer();
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				try {
					updateWords();
				} catch (Throwable t) {
					L.error(t);
				}
			}

		}, 0, config.getLong("t_p_product.updateWordsMinutes") * 60L * 1000L);
    }

    /**
     * 更新字典（词库）
     * @throws Exception
     */
	private void updateWords() throws Exception {
		// Add to words in db
		batchAddWords(loadBrandWords());

		// Update dict
		JdbcTemplate template = DataSourceManager.getJdbcTemplate();
		File dictFile = TokenUtil.getDictionaryFile();
		FileUtil.write(dictFile, StringUtil.EMPTY);
		int batch = 30;
		String lastId = null;
		while (true) {
			String sql = "select word from t_p_words";
			Object[] params = null;
			if (lastId != null) {
				sql += " where word>?";
				params = new Object[] { lastId };
			}
			sql += " order by word asc";
			List<String> words = template.findList(sql, params, 0, batch,
					StringPropertyHandler.getInstance());
			if (words.isEmpty()) {
				break;
			}
			lastId = words.get(words.size() - 1);
			StringBuilder buf = new StringBuilder();
			for (String word : words) {
				buf.append(word);
				buf.append(FileUtil.LINE);
			}
			FileUtil.append(dictFile, buf.toString());
		}

		// Reload
		TokenUtil.reloadDictionary();
    }

    /**
     *
     * @param words
     * @throws Exception
     */
	private void batchAddWords(Set<String> words) throws Exception {
		if (words.isEmpty()) {
			return;
		}
		JdbcTemplate template = DataSourceManager.getJdbcTemplate();
		final int batch = 50;
		while (!words.isEmpty()) {
			int i = 0;
			Set<Object> batchWords = new HashSet<Object>();
            StringBuilder sql = new StringBuilder("select word from t_p_words where word in(");

			for (Iterator<String> iter = words.iterator(); iter.hasNext();) {
				String word = iter.next();
				batchWords.add(word);
				iter.remove();
				if (i > 0) {
					sql.append(",?");
				} else {
					sql.append("?");
				}
				i++;
				if (i == batch) {
					break;
				}
			}
			sql.append(")");
			Object[] paramArray = new Object[batchWords.size()];
			batchWords.toArray(paramArray);
			List<String> items = template.findList(sql.toString(), paramArray,
					0, 0, StringPropertyHandler.getInstance());
			for (String item : items) {
				batchWords.remove(item);
			}
			if (batchWords.isEmpty()) {
				continue;
			}
			try {
				List<Object[]> params = new ArrayList<Object[]>(
						batchWords.size());
				for (Object batchWord : batchWords) {
					params.add(new Object[] { batchWord });
				}
				template.batchUpdate("insert into t_p_words(word) values(?)",
						params);
			} catch (Exception e) {
				L.error(e);
            }
        }

    }

    /**
     * 查询t_p_brand中的品牌中文名称
     * @return
     * @throws Exception
     */
	private Set<String> loadBrandWords() throws Exception {
		JdbcTemplate template = DataSourceManager.getJdbcTemplate();
		List<String> items = template.findList("select CNName from t_p_brand where status=1", null,
				0, 0, StringPropertyHandler.getInstance());
		Set<String> set = new HashSet<String>(items.size());

        TokenUtil.validateChineseCharAndAddToSet(items, set);
        return set;
	}
}
