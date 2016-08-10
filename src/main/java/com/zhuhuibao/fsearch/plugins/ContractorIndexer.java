package com.zhuhuibao.fsearch.plugins;

import java.io.File;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.zhuhuibao.fsearch.G;
import com.zhuhuibao.fsearch.service.MemberService;
import com.zhuhuibao.fsearch.service.dao.MemberDao;
import com.zhuhuibao.fsearch.util.Factor;
import com.zhuhuibao.fsearch.util.FormulaUtil;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import com.petkit.base.config.PropertiesConfig;
import com.petkit.base.repository.db.JdbcTemplate;
import com.petkit.base.repository.db.MapHandler;
import com.petkit.base.utils.CollectionUtil;
import com.petkit.base.utils.FileUtil;
import com.petkit.base.utils.FormatUtil;
import com.petkit.base.utils.StringUtil;
import com.zhuhuibao.fsearch.L;
import com.zhuhuibao.fsearch.core.DataSourceManager;
import com.zhuhuibao.fsearch.core.Indexer;
import com.zhuhuibao.fsearch.core.Searcher;
import com.zhuhuibao.fsearch.core.SearcherOptions;

public class ContractorIndexer implements Indexer {


    @Override
    public void init(SearcherOptions options, PropertiesConfig config)
            throws Exception {

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
                String sql = "select id,mobile,email,registerTime,if(status=10,'已认证','未认证') authinfo,enterpriseName,province,address,enterpriseType,"
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
                    List<Map<String, Object>> assetlevels = memberService.findCertLevel(id, "2");

//                    if (id == 140209 || id == 140217) {
//                        L.error(id + ">>>" + assetlevels.size());
//                    }

                    if (CollectionUtil.isNotEmpty(assetlevels)) {
                        for (Map<String, Object> map : assetlevels) {
                            String assetlevel = FormatUtil.parseString(map.get("certificate_name"));
                            if (map.get("certificate_grade") != null) {
                                assetlevel += FormatUtil.parseString(map.get("certificate_grade"));
                            }
                            docAsMap.put(assetlevel, assetlevel);
                        }
                        if (L.isInfoEnabled()) {
                            L.info(this.getClass() + "-----------caijl:contractor.assetlevels= " + StringUtil.join(assetlevels, ","));
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
            L.error("执行异常>>>", e);
            throw e;
        } finally {
            FileUtil.close(directory);
        }
    }

    /**
     * 排序规则
     *
     * @param docAsMap
     * @param assetlevels
     */
    private void genCertLevel(Map<String, Object> docAsMap, List<Map<String, Object>> assetlevels) {
        double certLevel;

//        Long id = FormatUtil.parseLong(docAsMap.get("id"));

        if (CollectionUtil.isNotEmpty(assetlevels)) {
            List<Factor> list = new ArrayList<>();
            for (Map<String, Object> map : assetlevels) {
                //企业资质权重=∑（资质权重×资质对应的“资质等级权重”）
                double certWeight, gradeWeight;
                String certName = FormatUtil.parseString(map.get("certificate_name"));
                String grade = FormatUtil.parseString(map.get("certificate_grade"));
                double certDefalutWeight = G.getConfig().getDouble("其他");
                if (StringUtil.isNotEmpty(certName.trim())) {
                    certWeight = G.getConfig().getDouble(certName.trim(), certDefalutWeight);
                } else {
                    certWeight = certDefalutWeight;
                }


                double gradeDefaultWeight = G.getConfig().getDouble("空值");
                if (StringUtil.isNotEmpty(grade.trim())) {
                    gradeWeight = G.getConfig().getDouble(grade.trim(), gradeDefaultWeight);
                } else {
                    gradeWeight = gradeDefaultWeight;
                }

//                if (id == 140209 || id == 140217) {
//                    L.error("[" + certName + "]:" + certWeight + " * " + "[" + grade + "]:" + gradeWeight);
//                }

                Factor factor = new Factor();
                factor.setWeight(certWeight);
                factor.setScore(gradeWeight);
                list.add(factor);
            }
            certLevel = FormulaUtil.calWeight(list);

//            if (id == 140209 || id == 140217) {
//                L.error("\t>>>{" + docAsMap.get("enterpriseName") + "}权重值:\t" + certLevel);
//            }

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
