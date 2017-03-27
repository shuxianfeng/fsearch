package com.movision.fsearch.plugins;

import com.movision.fsearch.L;
import com.movision.fsearch.analysis.TokenUtil;
import com.movision.fsearch.core.DataSourceManager;
import com.movision.fsearch.core.Indexer;
import com.movision.fsearch.core.Searcher;
import com.movision.fsearch.core.SearcherOptions;
import com.petkit.base.config.PropertiesConfig;
import com.petkit.base.repository.db.JdbcTemplate;
import com.petkit.base.repository.db.MultiTableMapHandler;
import com.petkit.base.repository.db.StringPropertyHandler;
import com.petkit.base.utils.FileUtil;
import com.petkit.base.utils.FormatUtil;
import com.petkit.base.utils.StringUtil;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import java.io.File;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Author zhuangyuhao
 * @Date 2017/3/17 20:59
 */
public class GoodsIndexer implements Indexer {

    private Timer timer = null;

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

        }, 0, config.getLong("yw_goods.updateWordsMinutes") * 60L * 1000L);
    }

    /**
     * 更新字典（词库）
     *
     * @throws Exception
     */
    private void updateWords() throws Exception {
        // Add to words in db
        this.batchAddWords(this.loadBrandWords());

        // Update dict
        JdbcTemplate template = DataSourceManager.getJdbcTemplate();
        File dictFile = TokenUtil.getDictionaryFile();
        FileUtil.write(dictFile, StringUtil.EMPTY);
        int batch = 30;
        String lastId = null;
        while (true) {
            String sql = "select word from t_goods_words";
            Object[] params = null;
            if (lastId != null) {
                sql += " where word>?";
                params = new Object[]{lastId};
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
     * 批量新增t_goods_words中的word
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
            StringBuilder sql = new StringBuilder("select word from t_goods_words where word in(");

            for (Iterator<String> iter = words.iterator(); iter.hasNext(); ) {
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
                    params.add(new Object[]{batchWord});
                }
                template.batchUpdate("insert into t_goods_words(word) values(?)",
                        params);
            } catch (Exception e) {
                L.error(e);
            }
        }

    }

    /**
     * 查询商品中文名称
     *
     * @return
     * @throws Exception
     */
    private Set<String> loadBrandWords() throws Exception {

        JdbcTemplate template = DataSourceManager.getJdbcTemplate();

        List<String> items = template.findList("select name from yw_goods", null,
                0, 0, StringPropertyHandler.getInstance());
        Set<String> set = new HashSet<String>(items.size());

        TokenUtil.validateChineseCharAndAddToSet(items, set);
        return set;
    }


    /**
     * 完整的索引过程
     *
     * @param searcher
     * @return
     * @throws Exception
     */
    @Override
    public Path fullIndex(Searcher searcher) throws Exception {

        //获取搜索选项
        SearcherOptions options = searcher.getOptions();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        File dir = new File(options.getPath() + "-" + dateFormat.format(new Date()));
        dir.mkdirs();
        Path path = dir.toPath();
        // 获取一个存储在文件系统中的索引的位置, 创建IndexWriter 应指向位置，其中索引是存储一个lucene的目录
        Directory directory = SimpleFSDirectory.open(path);

        try {
            //批量查询，每次300条数据
            int batch = 300;
            Object lastId = null;
            //实例化JdbcTemplate
            JdbcTemplate template = DataSourceManager.getJdbcTemplate();
            int total = 0;
            while (true) {
                /**
                 * 1 从数据库中查询所有数据
                 */
                Object[] params = null;
                //构建sql
                //yw_goods：商品名称、品牌、产品分类
                //产品分类：0 摄影机 1 摄像机 2 镜头 3 三脚架 4 滤镜 5 滑轨 6 轨道  7 灯具
                String sql = "SELECT g.*," +
                        "   (CASE g.protype" +
                        "   WHEN 0 THEN '摄影机'" +
                        "   WHEN 1 THEN '摄像机'" +
                        "   WHEN 2 THEN '镜头'" +
                        "   WHEN 3 THEN '三脚架'" +
                        "   WHEN 4 THEN '滤镜'" +
                        "   WHEN 5 THEN '滑轨'" +
                        "   WHEN 6 THEN '轨道'" +
                        "   WHEN 7 THEN '灯具'" +
                        "   END) AS protype_name, " +
                        "  b.brandname, i.img_url as img_url" +
                        "   FROM yw_goods g left join yw_brand b on b.brandid = g.brandid " +
                        " left join yw_goods_img i on i.goodsid= g.id and i.type=2 ";

                if (lastId != null) {
                    params = new Object[]{lastId};
                    sql += " where g.id>?";
                }
                //从小到大排序
                sql += " order by g.id asc";
                //获取数据库查询结果
                List<Map<String, Object>> docs = template.findList(sql, params,
                        0, batch, MultiTableMapHandler.CASESENSITIVE);
                if (docs.isEmpty()) {
                    break;
                }
                //获取结果中最后一条数据的id
                lastId = docs.get(docs.size() - 1).get("yw_goods.id");
                List<Document> documents = new ArrayList<Document>(docs.size());
                /**
                 * 2 遍历查询结果，并把搜索结果放入文档中
                 */
                for (Map<String, Object> docAsMap : docs) {
                    Map<String, Object> doc = new HashMap<String, Object>();

                    for (Map.Entry<String, Object> field : docAsMap.entrySet()) {

                        //这边把查询结果中的字段前的表名去掉，如yw_goods.id ——> id
                        /*String key = null;
                        if (field.getKey().startsWith("yw_brand.")) {
                            key = "brand_" + field.getKey().substring(field.getKey().indexOf('.') + 1);
                        }else if(field.getKey().startsWith("yw_goods_img.")){
                            key = "img_" + field.getKey().substring(field.getKey().indexOf('.') + 1);
                        }else{
                            key = field.getKey().substring(field.getKey().indexOf('.') + 1);
                        }*/
                        String key = field.getKey().substring(field.getKey().indexOf('.') + 1);

                        if (field.getValue() == null) {
                            continue;
                        }
                        doc.put(key, field.getValue());
                    }
                    //修改搜索的行数据
                    Map<String, Object> parsedDoc = parseRawDocument(doc);
                    //解析行数据，并放入文档
                    Document document = searcher.parseDocument(parsedDoc);
                    if (L.isInfoEnabled()) {
                        L.info(this.getClass() + " saving document: " + document);
                    }
                    documents.add(document);
                }
                /**
                 * 3 把搜索结果文档添加到索引中（用来创建索引）
                 */
                IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(Searcher.ANALYZER));
                /**
                 * 4 开始索引过程
                 */
                writer.addDocuments(documents);
                writer.close();
                total += documents.size();
            }
            L.warn(this.getClass() + " saved documents: " + total);
            return path;
        } catch (Exception e) {
            FileUtil.delete(path.toFile());
            L.error("执行异常>>>", e);
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
        //1 origprice 商品原价
        {
            Float origprice = FormatUtil.parseFloat(docAsMap.get("origprice"));
            if (null == origprice || origprice.floatValue() < 0) {
                origprice = Float.MAX_VALUE;
            }
            docAsMap.put("origprice1", origprice);
        }

        //2 price 商品折后价
        {
            Float price = FormatUtil.parseFloat(docAsMap.get("price"));
            if (null == price || price.floatValue() < 0) {
                price = Float.MAX_VALUE;
            }
            docAsMap.put("price1", price);
        }
        //发布时间 onlinetime
        {
            String publishtime = FormatUtil.parseString(docAsMap.get("onlinetime"));
            if (null != publishtime && publishtime.length() > 0) {
                //Java.util.Date该用什么field呢？这也是被问的频率比较高的一个问题，
                //  Lucene并没有提供DateField,请使用LongField代替，把Date转成毫秒数就OK了
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = sdf.parse(publishtime);
                publishtime = DateTools.dateToString(date, DateTools.Resolution.SECOND);
                long lpubtime = Long.valueOf(publishtime);
                docAsMap.put("onlinetime1", lpubtime);
            }
        }
        //品牌 brandid
        {
            Integer brandid = FormatUtil.parseInteger(docAsMap.get("brandid"));
            if (null != brandid && brandid.intValue() > 0) {
                docAsMap.put("brandid1", brandid);
            }
        }
        // protype 产品分类：0 摄影机 1 摄像机 2 镜头 3 三脚架 4 滤镜 5 滑轨 6 轨道  7 灯具
        {
            Integer protype = FormatUtil.parseInteger(docAsMap.get("protype"));
            if (null != protype && protype.intValue() > 0) {
                docAsMap.put("protype1", protype);
            }
        }
        return docAsMap;
    }


}
