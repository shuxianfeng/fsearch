package com.zhuhuibao.fsearch.plugins;

import java.io.File;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.zhuhuibao.fsearch.service.dao.MemberDao;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import com.petkit.base.config.PropertiesConfig;
import com.petkit.base.repository.db.JdbcTemplate;
import com.petkit.base.repository.db.MapHandler;
import com.petkit.base.repository.db.MultiTableMapHandler;
import com.petkit.base.utils.CollectionUtil;
import com.petkit.base.utils.FileUtil;
import com.petkit.base.utils.FormatUtil;
import com.petkit.base.utils.JSONUtil;
import com.petkit.base.utils.StringUtil;
import com.zhuhuibao.fsearch.L;
import com.zhuhuibao.fsearch.analysis.TokenUtil;
import com.zhuhuibao.fsearch.core.DataSourceManager;
import com.zhuhuibao.fsearch.core.Indexer;
import com.zhuhuibao.fsearch.core.SearchField;
import com.zhuhuibao.fsearch.core.Searcher;
import com.zhuhuibao.fsearch.core.SearcherOptions;

public class ContractorIndexer implements Indexer {

    private static final Map<String, String> ASSETLEVEL_MAP = new HashMap<>();

    @Override
    public void init(SearcherOptions options, PropertiesConfig config)
            throws Exception {
        //资质等级：1：一级；2：二级；3：三级；4：甲级；5：乙级
        ASSETLEVEL_MAP.put("A", "特级");
        ASSETLEVEL_MAP.put("B", "甲级");
        ASSETLEVEL_MAP.put("C", "乙级");
        ASSETLEVEL_MAP.put("ONE", "一级");
        ASSETLEVEL_MAP.put("TWO", "二级");
        ASSETLEVEL_MAP.put("THREE", "三级");
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
                String sql = "select id,mobile,email,registerTime,if(status=5,'已认证','未认证') authinfo,enterpriseName,province,address,enterpriseType,"
                        + "enterpriseLogo,enterpriseDesc,saleProductDesc,employeeNumber,enterpriseWebSite,enterpriseLinkman"
                        + " from t_m_member"
                        + " where status not in (0,2) and workType=100 and enterpriseEmployeeParentId=0 and identify like '%6%'";
                if (lastId != null) {
                    params = new Object[]{lastId};
                    sql += " and id>?";
                }
                sql += " order by id asc";
                List<Map<String, Object>> docs = template.findList(sql, params,
                        0, batch, MapHandler.CASESENSITIVE);
                if (docs.isEmpty()) {
                    break;
                }
                lastId = docs.get(docs.size() - 1).get("id");
                List<Document> documents = new ArrayList<Document>(docs.size());
                for (Map<String, Object> docAsMap : docs) {
                    Long id = FormatUtil.parseLong(docAsMap.get("id"));
                    Set<String> assetlevels = findAssetLevel(id);
                    if (CollectionUtil.isNotEmpty(assetlevels)) {
                        for (String assetlevel : assetlevels) {
                            docAsMap.put(assetlevel, assetlevel);
                        }
                        if (L.isInfoEnabled()) {
                            L.info(this.getClass() + "-----------caijl:contractor.assetlevels= "
                                    + StringUtil.join(assetlevels, ","));
                        }
                    }
                    Map<String, Object> doc = parseRawDocument(docAsMap);
                    Document document = searcher.parseDocument(doc);
                    //资质等级
                    for (Map.Entry<String, Object> entry : doc.entrySet()) {

                        String key = entry.getKey();
                        if (StringUtil.isNotEmpty(key)) {
                            Field field = (Field) document.getField(key);
                            if(field != null){
                                L.error(ASSETLEVEL_MAP.toString());
                                if (key.contains(ASSETLEVEL_MAP.get("A")) || key.contains(ASSETLEVEL_MAP.get("ONE"))) {
                                    field.setBoost(3L);
                                }
                                if (key.contains(ASSETLEVEL_MAP.get("B")) || key.contains(ASSETLEVEL_MAP.get("TWO"))) {
                                    field.setBoost(2L);
                                }
                                if (key.contains(ASSETLEVEL_MAP.get("C")) || key.contains(ASSETLEVEL_MAP.get("THREE"))) {
                                    field.setBoost(1L);
                                }
                            }
                        }
                    }


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
    public Map<String, Object> parseRawDocument(Map<String, Object> docAsMap)
            throws Exception {
        String registerTime = FormatUtil.parseString(docAsMap.get("registerTime"));
        if (null != registerTime && registerTime.length() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = sdf.parse(registerTime);
            registerTime = DateTools.dateToString(date, Resolution.SECOND);
            long lregtime = Long.valueOf(registerTime);
            docAsMap.put("registerTime1", lregtime);
        }

        MemberDao memberDao = new MemberDao();
        //viplevel
        String memberId = FormatUtil.parseString(docAsMap.get("id"));
        Map<String, Object> vipmember = memberDao.findVipMember(memberId);
        if (vipmember != null) {
            String vipLevel = FormatUtil.parseString(vipmember.get("vip_level"));
            docAsMap.put("viplevel", vipLevel);
        } else {
            docAsMap.put("viplevel", "");
        }

        return docAsMap;
    }


    public Set<String> findAssetLevel(Long memberId) throws Exception {
        Object lastId = null;
        JdbcTemplate template = DataSourceManager.getJdbcTemplate();
        int batch = 100;
        Set<String> keys = new HashSet<String>();
        while (true) {
            Object[] params = null;
            String sql = "select id,certificate_name,certificate_grade"
                    + " from t_certificate_record"
                    + " where type='2' and is_deleted=0 and status=1 and mem_id=?";
            if (lastId != null) {
                params = new Object[]{memberId, lastId};
                sql += " and id>?";
            } else {
                params = new Object[]{memberId};
            }
            sql += " order by id asc";
            List<Map<String, Object>> docs = template.findList(sql, params, 0,
                    batch, MapHandler.CASESENSITIVE);
            if (docs.isEmpty()) {
                break;
            }
            lastId = docs.get(docs.size() - 1).get("id");
            for (Map<String, Object> doc : docs) {
                String assetLevel = FormatUtil.parseString(doc.get("certificate_name"));
                if (doc.get("certificate_grade") != null) {
                    assetLevel += FormatUtil.parseString(doc.get("certificate_grade"));
                }
                keys.add(assetLevel);
            }
        }
        return keys;
    }

}
