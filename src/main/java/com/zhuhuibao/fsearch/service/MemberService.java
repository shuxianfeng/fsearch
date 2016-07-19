package com.zhuhuibao.fsearch.service;

import com.petkit.base.repository.db.JdbcTemplate;
import com.petkit.base.repository.db.MapHandler;
import com.petkit.base.repository.db.MultiTableMapHandler;
import com.petkit.base.utils.FormatUtil;
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


    public  void analysTime(Map<String, Object> docAsMap, String registerTime) throws ParseException {
        if (null != registerTime && registerTime.length() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = sdf.parse(registerTime);
            registerTime = DateTools.dateToString(date, DateTools.Resolution.SECOND);
            long lregtime = Long.valueOf(registerTime);
            docAsMap.put("registerTime1", lregtime);
        }
    }

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
                params = new Object[]{type,memberId, lastId};
                sql += " and id>?";
            } else {
                params = new Object[]{type,memberId};
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
}
