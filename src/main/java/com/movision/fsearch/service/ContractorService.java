package com.movision.fsearch.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.movision.fsearch.L;
import com.movision.fsearch.core.DataSourceManager;
import com.petkit.base.repository.db.JdbcTemplate;
import com.petkit.base.repository.db.MapHandler;
import com.petkit.base.utils.CollectionUtil;
import com.petkit.base.utils.StringUtil;
import com.movision.fsearch.core.SearchField;
import com.movision.fsearch.core.SearchFieldType;

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

				String degree = (String) certMap.get("degree");	//等级
				String name = (String) certMap.get("name");	//名称

				/**
				 *	若有名称+等级，并且等级有逗号分隔
				 */
				if (StringUtil.isNotBlank(name) && StringUtil.isNotBlank(degree) && degree.contains(",")) {

					String[] degreeArray = degree.split(",");

					for (String deg : degreeArray) {
						SearchField field = createSearchField(name + deg);	//名称+等级
						searchFieldList.add(field);
					}
				}
				/**
				 * 若有名称
				 */
				if(StringUtil.isNotBlank(name)){
					SearchField field = createSearchField(name);	//名称
					searchFieldList.add(field);
				}
			}
		}

		return searchFieldList;
	}

	/**
	 * 封装搜索字段
	 * @param name
	 * @return
	 */
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
			String sql = "SELECT id,name,type,degree  FROM t_dictionary_certificate WHERE TYPE = '2' AND searchFlag = '1' ORDER BY weight ASC";

			Object[] params = new Object[] {};

			resultlist = template.findList(sql, params, 0, Integer.MAX_VALUE, MapHandler.CASESENSITIVE);

		} catch (Exception e) {
			e.printStackTrace();
			L.error("ContractorDao:findContractorCertificate", e);
		}
		return resultlist;
	}

}
