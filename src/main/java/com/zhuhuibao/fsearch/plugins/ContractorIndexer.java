package com.zhuhuibao.fsearch.plugins;

import java.io.File;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.zhuhuibao.fsearch.service.MemberService;
import com.zhuhuibao.fsearch.service.dao.MemberDao;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.flexible.core.util.StringUtils;
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
                List<Document> documents = new ArrayList<>(docs.size());
                for (Map<String, Object> docAsMap : docs) {
                    Long id = FormatUtil.parseLong(docAsMap.get("id"));
                    MemberService memberService = new MemberService();
                    Set<String> assetlevels = memberService.findAssetLevel(id, "2");
                    if (CollectionUtil.isNotEmpty(assetlevels)) {
                        for (String assetlevel : assetlevels) {
                            docAsMap.put(assetlevel, assetlevel);
                        }
                        if (L.isInfoEnabled()) {
                            L.info(this.getClass() + "-----------caijl:contractor.assetlevels= "
                                    + StringUtil.join(assetlevels, ","));
                        }

                        genCertLevel(docAsMap, assetlevels);

                    } else {
                        docAsMap.put("certLevel", 0);
                    }
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

    private void genCertLevel(Map<String, Object> docAsMap, Set<String> assetlevels) {
        String certLevels = StringUtil.join(assetlevels, ",");
        //判断资质级别
        if (certLevels.contains(ASSETLEVEL_MAP.get("A")) || certLevels.contains(ASSETLEVEL_MAP.get("ONE"))) {
            int certLevel = 3;
            if (certLevels.contains(ASSETLEVEL_MAP.get("B")) || certLevels.contains(ASSETLEVEL_MAP.get("TWO"))) {
                certLevel++;
            }
            if (certLevels.contains(ASSETLEVEL_MAP.get("C")) || certLevels.contains(ASSETLEVEL_MAP.get("THREE"))) {
                certLevel++;
            }
            docAsMap.put("certLevel", certLevel);
        } else if (certLevels.contains(ASSETLEVEL_MAP.get("B")) || certLevels.contains(ASSETLEVEL_MAP.get("TWO"))) {
            int certLevel = 2;
            if(certLevels.contains(ASSETLEVEL_MAP.get("C")) || certLevels.contains(ASSETLEVEL_MAP.get("THREE"))){
                certLevel ++;
            }
            docAsMap.put("certLevel", certLevel);
        } else if (certLevels.contains(ASSETLEVEL_MAP.get("C")) || certLevels.contains(ASSETLEVEL_MAP.get("THREE"))) {
            int certLevel = 1;
            docAsMap.put("certLevel", certLevel);
        } else {
            docAsMap.put("certLevel", 0);
        }
    }

    @Override
    public Map<String, Object> parseRawDocument(Map<String, Object> docAsMap)
            throws Exception {
        MemberService memberService = new MemberService();
        String registerTime = FormatUtil.parseString(docAsMap.get("registerTime"));
        memberService.analysTime(docAsMap, registerTime);

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

}
