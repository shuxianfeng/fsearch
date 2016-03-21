package com.zhuhuibao.fsearch.core;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.petkit.base.config.PropertiesConfig;
import com.petkit.base.utils.BeanUtil;
import com.petkit.base.utils.FileUtil;
import com.petkit.base.utils.FileUtil.FileListHandler;
import com.petkit.base.utils.FormatUtil;
import com.petkit.base.utils.JSONUtil;
import com.zhuhuibao.fsearch.L;
import com.zhuhuibao.fsearch.exception.ApiException;

public class SearchManager {
	private static final Map<String, Searcher> SEARCHERS = new HashMap<String, Searcher>(
			0);
	static {
		try {
			FileUtil.listClassPathFiles(L.class, "conf/searchers",
					new FileListHandler() {

						@Override
						public void streamOpened(String fileName,
								String fullPath, InputStream in)
								throws Exception {
							if (fileName.startsWith("_")) {
								return;
							}
							PropertiesConfig config = new PropertiesConfig(in,
									false);
							Map<String, Object> map = config.getAll();
							String fieldsAsStr = map.remove("fields")
									.toString();
							SearcherOptions options = BeanUtil.map2bean(map,
									new SearcherOptions());
							options.setMaxDocsOfQuery(Math.min(
									options.getMaxDocsOfQuery(), 10000));
							{
								List<?> fields = JSONUtil
										.parseAsList(fieldsAsStr);
								List<SearchField> fieldList = new ArrayList<SearchField>(
										fields.size());
								for (Object field : fields) {
									Map<?, ?> fieldAsMap = (Map<?, ?>) field;
									String name = fieldAsMap.get("name")
											.toString();
									String typeAsStr = fieldAsMap.get("type")
											.toString();
									int type = getTypeAsInt(typeAsStr);
									boolean store = FormatUtil
											.parseBoolean(fieldAsMap
													.get("store"));
									boolean index = FormatUtil
											.parseBoolean(fieldAsMap
													.get("index"));
									boolean tokenized = false;
									boolean generalSearch = false;
									if (type == SearchFieldType.TYPE_STRING) {
										tokenized = FormatUtil
												.parseBoolean(fieldAsMap
														.get("tokenized"));
										generalSearch = FormatUtil
												.parseBoolean(fieldAsMap
														.get("generalSearch"));
									}
									boolean group = FormatUtil
											.parseBoolean(fieldAsMap
													.get("group"));
									SearchField f = new SearchField();
									f.setName(name);
									f.setType(type);
									f.setStore(store);
									f.setIndex(index);
									f.setTokenized(tokenized);
									f.setGeneralSearch(generalSearch);
									f.setGroup(group);
									if (!f.isIndex() && !f.isStore()
											&& !f.isGeneralSearch()) {
										throw new RuntimeException(
												"Neither store or index or generalsearch on field: "
														+ name);
									}
									fieldList.add(f);
								}
								options.setFields(fieldList);
							}

							SEARCHERS.put(options.getName(), new Searcher(
									options, config));
						}

						@Override
						public boolean willOpenStream(String fileName,
								String fullPath, boolean isDirectory)
								throws Exception {
							return true;
						}

					});
		} catch (Exception e) {
			L.error("Failed to init SearchManager", e);
			System.exit(-1);
		}
	}

	public static void init() {
		// Nope
	}

	private static int getTypeAsInt(String s) throws Exception {
		s = s.toLowerCase();
		if (s.equals("str") || s.equals("string")) {
			return SearchFieldType.TYPE_STRING;
		} else if (s.equals("int")) {
			return SearchFieldType.TYPE_INT;
		} else if (s.equals("long")) {
			return SearchFieldType.TYPE_LONG;
		} else if (s.equals("float")) {
			return SearchFieldType.TYPE_FLOAT;
		} else if (s.equals("double")) {
			return SearchFieldType.TYPE_DOUBLE;
		} else {
			throw new IllegalArgumentException("type: " + s);
		}
	}

	public static Searcher findSearcher(String name) {
		if (name == null) {
			return null;
		}
		return SEARCHERS.get(name);
	}

	public static Searcher getSearcher(String name) throws ApiException {
		Searcher searcher = findSearcher(name);
		if (searcher == null) {
			throw new ApiException("table.not.found");
		}
		return searcher;
	}
}