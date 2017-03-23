package com.movision.fsearch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.movision.fsearch.core.DataSourceManager;
import com.petkit.base.repository.db.JdbcTemplate;
import com.petkit.base.repository.db.MultiTableMapHandler;

public class JdbcTemplateTest {

	public static void main(String[] args) throws Exception {
		JdbcTemplate template = DataSourceManager.getJdbcTemplate();
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
                "   END) AS protypeName, b.brandname as brand_CNName" +
                "   FROM yw_goods g left join yw_brand b on b.brandid = g.brandid ";

		List<Map<String, Object>> docs = template.findList(sql, null, 0, 10,
				MultiTableMapHandler.CASESENSITIVE);

		for (Map<String, Object> docAsMap : docs) {

            Map<String, Object> doc = new HashMap<String, Object>();
			for (Entry<String, Object> field : docAsMap.entrySet()) {
				String key = null;

                if (field.getKey().startsWith("yw_brand.")) {
                    key = "brand_"
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
