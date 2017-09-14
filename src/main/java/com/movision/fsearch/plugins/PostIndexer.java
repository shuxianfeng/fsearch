package com.movision.fsearch.plugins;

import com.movision.fsearch.L;
import com.movision.fsearch.core.DataSourceManager;
import com.movision.fsearch.core.Indexer;
import com.movision.fsearch.core.Searcher;
import com.movision.fsearch.core.SearcherOptions;
import com.petkit.base.config.PropertiesConfig;
import com.petkit.base.repository.db.JdbcTemplate;
import com.petkit.base.repository.db.MultiTableMapHandler;
import com.petkit.base.utils.FileUtil;
import com.petkit.base.utils.FormatUtil;
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
 * @Date 2017/3/27 15:09
 */
public class PostIndexer implements Indexer {

    @Override
    public void init(SearcherOptions options, PropertiesConfig config)
            throws Exception {

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
                String sql = " select p.*, d.begintime, d.endtime, c.name, " +
                        " u.nickname " +
                        " from yw_post p left join yw_active_period d on p.id=d.postid " +
                        " left join yw_circle c on c.id=p.circleid " +
                        " left join yw_user u on u.id=p.userid  " +
                        " where p.isdel = 0";

                if (lastId != null) {
                    params = new Object[]{lastId};
                    sql += " and p.id>? ";
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
                lastId = docs.get(docs.size() - 1).get("yw_post.id");
                List<Document> documents = new ArrayList<Document>(docs.size());
                /**
                 * 2 遍历查询结果，并把搜索结果放入文档中
                 */
                for (Map<String, Object> docAsMap : docs) {
                    Map<String, Object> doc = new HashMap<String, Object>();

                    for (Map.Entry<String, Object> field : docAsMap.entrySet()) {

                        //这边把查询结果中的字段前的表名去掉，如yw_goods.id ——> id
                        String key = field.getKey().substring(field.getKey().indexOf('.') + 1);
                        //针对圈子名称特殊处理
                        if (key.equals("name")) {
                            key = "circlename";
                        }

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
        //发布时间 intime
        {
            String intime = FormatUtil.parseString(docAsMap.get("intime"));
            if (null != intime && intime.length() > 0) {
                //Java.util.Date该用什么field呢？这也是被问的频率比较高的一个问题，
                //  Lucene并没有提供DateField,请使用LongField代替，把Date转成毫秒数就OK了
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = sdf.parse(intime);
                intime = DateTools.dateToString(date, DateTools.Resolution.SECOND);
                long lpubtime = Long.valueOf(intime);
                docAsMap.put("intime1", lpubtime);
            }
        }
        // 活动开始时间 begintime
        {
            String begintime = FormatUtil.parseString(docAsMap.get("begintime"));
            if (null != begintime && begintime.length() > 0) {
                //Java.util.Date该用什么field呢？这也是被问的频率比较高的一个问题，
                //  Lucene并没有提供DateField,请使用LongField代替，把Date转成毫秒数就OK了
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = sdf.parse(begintime);
                begintime = DateTools.dateToString(date, DateTools.Resolution.SECOND);
                long lpubtime = Long.valueOf(begintime);
                docAsMap.put("begintime1", lpubtime);
            }
        }
        //活动结束时间 endtime
        {
            String endtime = FormatUtil.parseString(docAsMap.get("endtime"));
            if (null != endtime && endtime.length() > 0) {
                //Java.util.Date该用什么field呢？这也是被问的频率比较高的一个问题，
                //  Lucene并没有提供DateField,请使用LongField代替，把Date转成毫秒数就OK了
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = sdf.parse(endtime);
                endtime = DateTools.dateToString(date, DateTools.Resolution.SECOND);
                long lpubtime = Long.valueOf(endtime);
                docAsMap.put("endtime1", lpubtime);
            }
        }

        return docAsMap;
    }

}
