package com.zhuhuibao.fsearch.service;

import com.petkit.base.repository.db.JdbcTemplate;
import com.petkit.base.repository.db.MapHandler;
import com.petkit.base.repository.db.MultiTableMapHandler;
import com.petkit.base.utils.FormatUtil;
import com.zhuhuibao.fsearch.L;
import com.zhuhuibao.fsearch.core.DataSourceManager;
import org.apache.lucene.document.DateTools;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author jianglz
 * @since 16/7/18.
 */
public class MemberService {


    public void analysTime(Map<String, Object> docAsMap, String registerTime) throws ParseException {
        if (null != registerTime && registerTime.length() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = sdf.parse(registerTime);
            registerTime = DateTools.dateToString(date, DateTools.Resolution.SECOND);
            long lregtime = Long.valueOf(registerTime);
            docAsMap.put("registerTime1", lregtime);
        }
    }

    /**
     * 查询用户资质信息
     *
     * @param memberId 用户ID
     * @param type     资质类型：1：供应商资质；2：工程商资质；3：个人资质
     * @return
     * @throws Exception
     */
    public Set<String> findAssetLevel(Long memberId, String type) throws Exception {
        Object lastId = null;
        JdbcTemplate template = DataSourceManager.getJdbcTemplate();
        int batch = 100;
        Set<String> keys = new HashSet<>();
        while (true) {
            Object[] params;
            String sql = "select id,certificate_name,certificate_grade"
                    + " from t_certificate_record"
                    + " where type= ? and is_deleted=0 and status=1 and mem_id=?";
            if (lastId != null) {
                params = new Object[]{type, memberId, lastId};
                sql += " and id>?";
            } else {
                params = new Object[]{type, memberId};
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


    /**
     * 用户产品类别信息
     *
     * @param memberId 用户ID
     * @return
     * @throws Exception
     */
    public Set<String> findCategory(Long memberId) throws Exception {
        Object lastId = null;
        JdbcTemplate template = DataSourceManager.getJdbcTemplate();
        int batch = 100;
        Set<String> keys = new HashSet<>();
        while (true) {
            Object[] params;
            String sql = "select c.id,c.name"
                    + " from t_p_category c,(select distinct scateid from t_p_product where status=1 and createid=?) p"
                    + " where c.id=p.scateid";
            if (lastId != null) {
                params = new Object[]{memberId, lastId};
                sql += " and c.id>?";
            } else {
                params = new Object[]{memberId};
            }
            sql += " order by c.id asc";
            List<Map<String, Object>> docs = template.findList(sql, params, 0,
                    batch, MultiTableMapHandler.CASESENSITIVE);
            if (docs.isEmpty()) {
                break;
            }
            lastId = docs.get(docs.size() - 1).get("t_p_category.id");
            for (Map<String, Object> doc : docs) {
                String categoryName = FormatUtil.parseString(doc.get("t_p_category.name"));
                keys.add(categoryName);
            }
        }
        return keys;
    }


    /**
     * 用户产品信息
     *
     * @param memberId 用户ID
     * @return
     * @throws Exception
     */
    public Set<Map<String, Object>> findProducts(Long memberId) throws Exception {
        Object lastId = null;
        JdbcTemplate template = DataSourceManager.getJdbcTemplate();
        int batch = 100;
        Set<Map<String, Object>> keys = new HashSet<>();
        try {
            while (true) {
                Object[] params;
                String sql = "select id,name from t_p_product where status=1 and createid= ?";
                if (lastId != null) {
                    params = new Object[]{memberId, lastId};
                    sql += " and id>?";
                } else {
                    params = new Object[]{memberId};
                }
                sql += " order by id asc";
                List<Map<String, Object>> list = template.findList(sql, params, 1, batch, MapHandler.CASESENSITIVE);
                if (list.isEmpty()) {
                    break;
                }
                lastId = list.get(list.size() - 1).get("id");
                for (Map<String, Object> doc : list) {
                    keys.add(doc);
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
            L.error("[t_p_product]查询失败:" + e.getMessage());
        }

        return keys;
    }

    /**
     * 用户成功案例
     *
     * @param memberId 用户ID
     * @return
     * @throws Exception
     */
    public Set<Map<String, Object>> findSuccesscase(Long memberId) throws Exception {
        Object lastId = null;
        JdbcTemplate template = DataSourceManager.getJdbcTemplate();
        int batch = 100;
        Set<Map<String, Object>> keys = new HashSet<>();
        try {
            while (true) {
                Object[] params;
                String sql = "select * from t_m_success_case where `status` = 1 and is_deleted=0 and createid=?";
                if (lastId != null) {
                    params = new Object[]{memberId, lastId};
                    sql += " and id>?";
                } else {
                    params = new Object[]{memberId};
                }
                sql += " order by id asc";
                List<Map<String, Object>> list = template.findList(sql, params, 1, batch, MapHandler.CASESENSITIVE);
                if (list.isEmpty()) {
                    break;
                }
                lastId = list.get(list.size() - 1).get("id");
                for (Map<String, Object> doc : list) {
                    keys.add(doc);
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
            L.error("[t_m_success_case]查询失败:" + e.getMessage());
        }

        return keys;
    }

    public static void main(String[] args) throws Exception {
        MemberService service = new MemberService();
        Set<Map<String, Object>>  products =service.findProducts(15501L);
        System.out.println("产品数量:"+products.size());
        for(Map<String,Object> map : products){
            System.out.println(map.get("name"));
        }

        Set<Map<String, Object>> cases = service.findSuccesscase(11L);
        System.out.println("成功案例数量:"+cases.size());

        Set<String>  categorys  = service.findCategory(15501L);
        System.out.println("产品分类数量:"+categorys.size());

       Set<String> levels =  service.findAssetLevel(17647L,"1");
        System.out.println("资质数量:"+levels.size());
    }

}
