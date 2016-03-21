package com.zhuhuibao.fsearch.core;

public class SearchField {
	private String name;
	private int type;
	private boolean store;
	private boolean index = true;
	private boolean tokenized = true;
	private boolean generalSearch;
	private boolean group = false;

	public SearchField() {
		super();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public boolean isStore() {
		return store;
	}

	public void setStore(boolean store) {
		this.store = store;
	}

	public boolean isIndex() {
		return index;
	}

	public void setIndex(boolean index) {
		this.index = index;
	}

	public boolean isTokenized() {
		return tokenized;
	}

	public void setTokenized(boolean tokenized) {
		this.tokenized = tokenized;
	}

	public boolean isGeneralSearch() {
		return generalSearch;
	}

	public void setGeneralSearch(boolean generalSearch) {
		this.generalSearch = generalSearch;
	}

	public boolean isGroup() {
		return group;
	}

	public void setGroup(boolean group) {
		this.group = group;
	}

}
