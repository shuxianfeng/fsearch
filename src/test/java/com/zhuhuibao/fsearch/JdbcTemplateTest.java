package com.zhuhuibao.fsearch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.petkit.base.repository.db.JdbcTemplate;
import com.petkit.base.repository.db.MultiTableMapHandler;
import com.zhuhuibao.fsearch.core.DataSourceManager;

public class JdbcTemplateTest {

	public static void main(String[] args) throws Exception {
		JdbcTemplate template = DataSourceManager.getJdbcTemplate();
		String sql = "select p.*, m.enterpriseName member_enterpriseName, m.companyIdentify member_companyIdentify, m.province member_province, m.city member_city, b.brandCNName brand_brandCNName from t_p_product p left outer join t_m_member m on m.id=p.createid left outer join t_p_brand b on b.id=p.brandid";
		List<Map<String, Object>> docs = template.findList(sql, null, 0, 10,
				MultiTableMapHandler.CASESENSITIVE);
		for (Map<String, Object> docAsMap : docs) {
			Map<String, Object> doc = new HashMap<String, Object>();
			for (Entry<String, Object> field : docAsMap.entrySet()) {
				String key = null;
				if (field.getKey().startsWith("t_m_member.")) {
					key = "member_"
							+ field.getKey().substring(
									field.getKey().indexOf('.') + 1);
				} else {
					key = field.getKey().substring(
							field.getKey().indexOf('.') + 1);
				}
				if (field.getValue() == null) {
					continue;
				}
				doc.put(key, field.getValue());
			}
			System.out.println(doc);
		}
	}
}
