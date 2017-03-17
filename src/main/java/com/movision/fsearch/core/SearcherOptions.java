package com.movision.fsearch.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 搜索选项实体类
 * @author zhuangyuhao
 * @time   2016年11月10日 下午8:35:02
 *
 */
public class SearcherOptions {
	private String name;
	private String path;
	private String idField;
	private String generalSearchField;
	private int maxDocsOfQuery = 1000;
	private List<SearchField> fields;
	private Map<String, SearchField> fieldMap;
	private SearchField idFieldObject;
	// full index
	private String fullIndexer;
	private boolean fullIndexAtStart;
	private int fullIndexInterval;
	private int groupItemCount;

	private void renderFieldInfo() {
		fieldMap = new HashMap<String, SearchField>(fields.size());
		for (SearchField field : fields) {
			fieldMap.put(field.getName(), field);
			if (field.getName().equals(idField)) {
				idFieldObject = field;
			}
		}
	}

	public SearchField getIdFieldObject() {
		return idFieldObject;
	}

	public SearchField findField(String name) {
		return fieldMap.get(name);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getIdField() {
		return idField;
	}

	public void setIdField(String idField) {
		this.idField = idField;
	}

	public String getGeneralSearchField() {
		return generalSearchField;
	}

	public void setGeneralSearchField(String generalSearchField) {
		this.generalSearchField = generalSearchField;
	}

	public int getMaxDocsOfQuery() {
		return maxDocsOfQuery;
	}

	public void setMaxDocsOfQuery(int maxDocsOfQuery) {
		this.maxDocsOfQuery = maxDocsOfQuery;
	}

	public List<SearchField> getFields() {
		return fields;
	}

	public void setFields(List<SearchField> fields) {
		this.fields = fields;
		renderFieldInfo();
	}

	public int getFullIndexInterval() {
		return fullIndexInterval;
	}

	public void setFullIndexInterval(int fullIndexInterval) {
		this.fullIndexInterval = fullIndexInterval;
	}

	public String getFullIndexer() {
		return fullIndexer;
	}

	public void setFullIndexer(String fullIndexer) {
		this.fullIndexer = fullIndexer;
	}

	public boolean isFullIndexAtStart() {
		return fullIndexAtStart;
	}

	public void setFullIndexAtStart(boolean fullIndexAtStart) {
		this.fullIndexAtStart = fullIndexAtStart;
	}

	public int getGroupItemCount() {
		return groupItemCount;
	}

	public void setGroupItemCount(int groupItemCount) {
		this.groupItemCount = groupItemCount;
	}

}
