package com.zhuhuibao.fsearch.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.petkit.base.repository.db.JdbcTemplate;
import com.petkit.base.repository.db.MapHandler;
import com.petkit.base.utils.CollectionUtil;
import com.petkit.base.utils.StringUtil;
import com.sun.java.swing.plaf.windows.WindowsTreeUI.CollapsedIcon;
import com.zhuhuibao.fsearch.L;
import com.zhuhuibao.fsearch.core.DataSourceManager;
import com.zhuhuibao.fsearch.core.SearchField;
import com.zhuhuibao.fsearch.core.SearchFieldType;

/**
 * 
 * @author tongxl
 *
 */
public class ContractorService {

	/**
	 * 构建资质搜索字段
	 * 
	 * @return
	 */
	public List<SearchField> findContractorCertificateSearchField() {
		List<SearchField> searchFieldList = new ArrayList<SearchField>();

		List<Map<String, Object>> certificateList = findContractorCertificate();
		if (CollectionUtil.isNotEmpty(certificateList)) {
			for (Map<String, Object> certMap : certificateList) {
				String degree = (String) certMap.get("degree");
				String name = (String) certMap.get("name");
				if (StringUtil.isNotBlank(name) && StringUtil.isNotBlank(degree) && degree.contains(",")) {
					String[] degreeArray = degree.split(",");
					for (String deg : degreeArray) {
						SearchField field = createSearchField(name + deg);
						searchFieldList.add(field);
					}
				}
				
				if(StringUtil.isNotBlank(name)){
					SearchField field = createSearchField(name);
					searchFieldList.add(field);
				}
			}
		}

		return searchFieldList;
	}

	private SearchField createSearchField(String name) {
		if (StringUtil.isBlank(name)) {
			return null;
		}
		SearchField field = new SearchField();
		field.setTokenized(false);
		field.setName(name);
		field.setType(SearchFieldType.TYPE_STRING);
		field.setStore(false);
		field.setIndex(true);
		field.setTokenized(false);
		field.setGeneralSearch(false);
		field.setGroup(false);

		return field;
	}

	/**
	 * 查询所有参与搜索的资质信息
	 * 
	 * @return
	 */
	public List<Map<String, Object>> findContractorCertificate() {
		List<Map<String, Object>> resultlist = new ArrayList<Map<String, Object>>();
		JdbcTemplate template = DataSourceManager.getJdbcTemplate();

		try {
			String sql = "SELECT id,name,type,degree  FROM t_dictionary_certificate WHERE TYPE = '2'";

			Object[] params = new Object[] {};

			resultlist = template.findList(sql, params, 0, Integer.MAX_VALUE, MapHandler.CASESENSITIVE);

		} catch (Exception e) {
			e.printStackTrace();
			L.error("ContractorDao:findContractorCertificate", e);
		}
		return resultlist;
	}

}
