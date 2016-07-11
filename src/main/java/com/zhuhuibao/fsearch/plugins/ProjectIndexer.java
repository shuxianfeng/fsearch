package com.zhuhuibao.fsearch.plugins;

import com.petkit.base.config.PropertiesConfig;
import com.petkit.base.repository.db.JdbcTemplate;
import com.petkit.base.repository.db.MapHandler;
import com.petkit.base.repository.db.StringPropertyHandler;
import com.petkit.base.utils.FileUtil;
import com.petkit.base.utils.FormatUtil;
import com.petkit.base.utils.StringUtil;
import com.zhuhuibao.fsearch.L;
import com.zhuhuibao.fsearch.core.DataSourceManager;
import com.zhuhuibao.fsearch.core.Indexer;
import com.zhuhuibao.fsearch.core.Searcher;
import com.zhuhuibao.fsearch.core.SearcherOptions;
import com.zhuhuibao.fsearch.service.dao.ProjectDao;
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
 * 项目信息索引
 */
public class ProjectIndexer implements Indexer {


    @Override
    public void init(SearcherOptions options, PropertiesConfig config) throws Exception {

    }

    @Override
    public Path fullIndex(Searcher searcher) throws Exception {
        SearcherOptions options = searcher.getOptions();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        File dir = new File(options.getPath() + "-"
                + dateFormat.format(new Date()));
        dir.mkdirs();
        Path path = dir.toPath();
        Directory directory = SimpleFSDirectory.open(path);

        try {
            int batch = 300;
            Object lastId = null;
            JdbcTemplate template = DataSourceManager.getJdbcTemplate();
            int total = 0;

            while (true) {
                Object[] params = null;
                String sql = "select " +
                        "        t.id ,t.name,date_format(t.publishDate,'%Y-%m-%d') publishDate," +
                        "        date_format(t.updateDate,'%Y-%m-%d') updateDate,t.address,t.province,t.city,t.area,t.category," +
                        "        t.price,date_format(t.startDate,'%Y-%m-%d') startDate,date_format(t.endDate,'%Y-%m-%d') endDate" +
                        "    from t_prj_project t";
                if (lastId != null) {
                    params = new Object[]{lastId};
                    sql += "  where t.id>?";
                }
                sql += " order by t.id asc";
                List<Map<String, Object>> docs = template.findList(sql, params,
                        0, batch, MapHandler.CASESENSITIVE);
                if (docs.isEmpty()) {
                    break;
                }
                lastId = docs.get(docs.size() - 1).get("id");

                List<Document> documents = new ArrayList<>(docs.size());
                for (Map<String, Object> docAsMap : docs) {

                    Map<String, Object> doc = parseRawDocument(docAsMap);

                    Document document = searcher.parseDocument(doc);
                    if (L.isInfoEnabled()) {
                        L.info(this.getClass() + " saving document: "
                                + document);
                    }
                    documents.add(document);
                }
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
            throw e;
        } finally {
            FileUtil.close(directory);
        }
    }

    @Override
    public Map<String, Object> parseRawDocument(Map<String, Object> docAsMap) throws Exception {

        String updateDate = FormatUtil.parseString(docAsMap.get("updateDate"));
        if(null != updateDate && updateDate.length() > 0 ){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date = sdf.parse(updateDate);
            updateDate = DateTools.dateToString(date, DateTools.Resolution.DAY);
            long upDate= Long.valueOf(updateDate);
            docAsMap.put("updateDate", upDate);
        }
        String startDate = FormatUtil.parseString(docAsMap.get("startDate"));
        if(null != startDate && startDate.length() > 0 ){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date = sdf.parse(startDate);
            startDate = DateTools.dateToString(date, DateTools.Resolution.DAY);
            long sDate= Long.valueOf(startDate);
            docAsMap.put("startDate", sDate);
        }
        String endDate = FormatUtil.parseString(docAsMap.get("endDate"));
        if(null != endDate && endDate.length() > 0 ){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date = sdf.parse(endDate);
            endDate = DateTools.dateToString(date, DateTools.Resolution.DAY);
            long eDate= Long.valueOf(endDate);
            docAsMap.put("endDate", eDate);
        }

        ProjectDao projectDao = new ProjectDao();

        //转换city >>>> cityName
        String city = FormatUtil.parseString(docAsMap.get("city"));
        String cityName =projectDao. findCityName(city);
        docAsMap.put("cityName",cityName);

        //转换category >>> categoryName
        String category = FormatUtil.parseString(docAsMap.get("category"));
        String categoryName = projectDao.findCategoryNames(category);
        docAsMap.put("categoryName",categoryName);

        String province =  FormatUtil.parseString(docAsMap.get("province"));
        String area =  FormatUtil.parseString(docAsMap.get("area"));
        String address =  FormatUtil.parseString(docAsMap.get("address"));
        //转换address >>>> provinceName + cityName + areaName + address
        String newAddress = projectDao.findAddress(province,cityName,area,address);
        docAsMap.put("address",newAddress);

        return docAsMap;
    }


}

