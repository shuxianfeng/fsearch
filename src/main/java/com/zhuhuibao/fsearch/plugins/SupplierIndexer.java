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

import com.zhuhuibao.fsearch.G;
import com.zhuhuibao.fsearch.service.MemberService;
import com.zhuhuibao.fsearch.service.dao.MemberDao;
import com.zhuhuibao.fsearch.util.Factor;
import com.zhuhuibao.fsearch.util.FormulaUtil;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
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

public class SupplierIndexer implements Indexer {

    private static double REGISTER_CAPITAL = 0;
    private static double PRODUCT_NUM = 0;
    private static double PRODUCT_CATEGORY_NUM = 0;
    private static double SUCCASE_NUM = 0;
    private static double LEVEL_NUM = 0;

    @Override
    public void init(SearcherOptions options, PropertiesConfig config)
            throws Exception {
        //初始化排序规则
        REGISTER_CAPITAL = G.getConfig().getDouble("sppplier.weight.register_capital");    //注册资本
        PRODUCT_NUM = G.getConfig().getDouble("sppplier.weight.product_num"); //产品数量
        PRODUCT_CATEGORY_NUM = G.getConfig().getDouble("sppplier.weight.product_category_num");      //产品分类数量
        SUCCASE_NUM = G.getConfig().getDouble("sppplier.weight.succase_num");                //成功案例数量
        LEVEL_NUM = G.getConfig().getDouble("sppplier.weight.level_num");              //荣誉资质数量
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
                String sql = "select id,mobile,email,registerTime,if(status=5,'已认证','未认证') authinfo,identify,enterpriseName,province,address,enterpriseType,"
                        + "enterpriseLogo,enterpriseDesc,saleProductDesc,employeeNumber,enterpriseWebSite,enterpriseLinkman,registerCapital,currency"
                        + " from t_m_member"
                        + " where status not in (0,2) and workType=100 and enterpriseEmployeeParentId=0 and (identify like '%3%' or  identify like '%4%'  or identify like '%5%')";
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
                    Set<String> assetlevels = memberService.findAssetLevel(id, "1");
                    if (CollectionUtil.isNotEmpty(assetlevels)) {
                        for (String assetlevel : assetlevels) {
                            docAsMap.put(assetlevel, assetlevel);
                        }
                        if (L.isInfoEnabled()) {
                            L.info(this.getClass() + "-----------caijl:supplier.assetlevels= "
                                    + StringUtil.join(assetlevels, ","));
                        }
                    }
                    Set<String> categorys = memberService.findCategory(id);
                    if (CollectionUtil.isNotEmpty(categorys)) {
                        for (String category : categorys) {
                            docAsMap.put(category, category);
                        }
                        if (L.isInfoEnabled()) {
                            L.info(this.getClass() + "-----------caijl:supplier.categorys= "
                                    + StringUtil.join(categorys, ","));
                        }
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

    @Override
    public Map<String, Object> parseRawDocument(Map<String, Object> docAsMap)
            throws Exception {
        MemberService memberService = new MemberService();
        String memberId = FormatUtil.parseString(docAsMap.get("id"));

        //registerTime
        {
            String registerTime = FormatUtil.parseString(docAsMap.get("registerTime"));
            memberService.analysTime(docAsMap, registerTime);
        }
        {
            String identify = FormatUtil.parseString(docAsMap.get("identify"));
            if (identify.contains("3")) {
                docAsMap.put("3", "3");
            }
            if (identify.contains("4")) {
                docAsMap.put("4", "4");
            }
            if (identify.contains("5")) {
                docAsMap.put("5", "5");
            }
        }
        //viplevel
        {
            MemberDao memberDao = new MemberDao();
            Map<String, Object> vipmember = memberDao.findVipMember(memberId);
            if (vipmember != null) {
                String vipLevel = FormatUtil.parseString(vipmember.get("vip_level"));
                docAsMap.put("viplevel", vipLevel);
            } else {
                docAsMap.put("viplevel", "");
            }
        }
        //weightLevel
        {
            List<Factor> factors = new ArrayList<>();
            Factor factor = new Factor();
            //注册资本     registerCapital,currency
            Double registerCapital = FormatUtil.parseDouble(docAsMap.get("registerCapital"));
            double capital = registerCapital == null ? 0 : registerCapital;
//            System.err.println("注册资金:" + capital);
            String currency = FormatUtil.parseString(docAsMap.get("currency"));//货币类型：1人民币，2美元
            if ("2".equals(currency)) {
                capital = FormulaUtil.mul(capital, 6.6);
            }
            double score = FormulaUtil.div(capital, 1000, 2);
            factor.setWeight(REGISTER_CAPITAL);
            factor.setScore(score);
            factors.add(factor);

            Set<Map<String, Object>> products = memberService.findProducts(FormatUtil.parseLong(memberId));
            int productsNum = products.size();
//            System.err.println("产品数量:" + productsNum);
            //产品数量
            factor = new Factor();
            factor.setWeight(PRODUCT_NUM);
            factor.setScore(productsNum);
            factors.add(factor);

            Set<Map<String, Object>> cases = memberService.findSuccesscase(FormatUtil.parseLong(memberId));
            int casesNum = cases.size();
//            System.err.println("成功案例数量:" + casesNum);
            //成功案例数量
            factor = new Factor();
            factor.setWeight(SUCCASE_NUM);
            factor.setScore(casesNum);
            factors.add(factor);

            Set<String> categorys = memberService.findCategory(FormatUtil.parseLong(memberId));
            int categorysNum = categorys.size();
//            System.err.println("产品分类数量:" + categorysNum);
            //产品分类数量
            factor = new Factor();
            factor.setWeight(PRODUCT_CATEGORY_NUM);
            factor.setScore(categorysNum);
            factors.add(factor);

            Set<String> levels = memberService.findAssetLevel(FormatUtil.parseLong(memberId), "1");  //供应商资质
            int levelsNum = levels.size();
//            System.err.println("资质数量:" + levelsNum);
            //资质数量
            factor = new Factor();
            factor.setWeight(LEVEL_NUM);
            factor.setScore(levelsNum);
            factors.add(factor);

            double result = FormulaUtil.calWeight(factors);
            docAsMap.put("weightLevel", result);
//            System.err.println("权重值:"+result);
        }

        return docAsMap;
    }


}
