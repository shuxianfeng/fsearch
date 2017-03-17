package com.movision.fsearch.core;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.movision.fsearch.L;
import com.movision.fsearch.exception.ApiException;
import com.movision.fsearch.service.ContractorService;
import com.petkit.base.config.PropertiesConfig;
import com.petkit.base.utils.BeanUtil;
import com.petkit.base.utils.FileUtil;
import com.petkit.base.utils.FileUtil.FileListHandler;
import com.petkit.base.utils.FormatUtil;
import com.petkit.base.utils.JSONUtil;

public class SearchManager {

    private static final Map<String, Searcher> SEARCHERS = new HashMap<>(0);

    /**
     * 初始化SEARCHERS
     */
    static {
        try {
            FileUtil.listClassPathFiles(L.class, "conf/searchers", new FileListHandler() {

                @Override
                public void streamOpened(String fileName, String fullPath, InputStream in) throws Exception {
                    if (fileName.startsWith("_")) {
                        return;
                    }
                    PropertiesConfig config = new PropertiesConfig(in, false);
                    Map<String, Object> map = config.getAll();

                    String fieldsAsStr = map.remove("fields").toString();
                    //封装SearcherOptions
                    SearcherOptions options = BeanUtil.map2bean(map, new SearcherOptions());
                    options.setMaxDocsOfQuery(Math.min(options.getMaxDocsOfQuery(), 10000));
                    {
                        List<?> fields = JSONUtil.parseAsList(fieldsAsStr);
                        List<SearchField> fieldList = new ArrayList<SearchField>(fields.size());
                        for (Object field : fields) {
                            Map<?, ?> fieldAsMap = (Map<?, ?>) field;
                            String name = fieldAsMap.get("name").toString();
                            String typeAsStr = fieldAsMap.get("type").toString();
                            int type = getTypeAsInt(typeAsStr);
                            boolean store = FormatUtil.parseBoolean(fieldAsMap.get("store"));
                            boolean index = FormatUtil.parseBoolean(fieldAsMap.get("index"));
                            boolean tokenized = false;
                            boolean generalSearch = false;
                            if (type == SearchFieldType.TYPE_STRING) {
                                tokenized = FormatUtil.parseBoolean(fieldAsMap.get("tokenized"));
                                generalSearch = FormatUtil.parseBoolean(fieldAsMap.get("generalSearch"));
                            }
                            boolean group = FormatUtil.parseBoolean(fieldAsMap.get("group"));
                            //封装SearchField对象
                            SearchField f = new SearchField();
                            f.setName(name);
                            f.setType(type);
                            f.setStore(store);
                            f.setIndex(index);
                            f.setTokenized(tokenized);
                            f.setGeneralSearch(generalSearch);
                            f.setGroup(group);
                            if (!f.isIndex() && !f.isStore() && !f.isGeneralSearch()) {
                                throw new RuntimeException("Neither store or index or generalsearch on field: " + name);
                            }
                            fieldList.add(f);
                        }

                        // todo 若当前处理工程商搜索，则增加工程商资质相关字段
                        if ("contractor".equals(options.getName())) {
                            ContractorService contractorService = new ContractorService();
                            //构建资质搜索字段
                            List<SearchField> certificateSearchFieldList = contractorService.findContractorCertificateSearchField();
                            fieldList.addAll(certificateSearchFieldList);
                        }

                        options.setFields(fieldList);
                    }

                    SEARCHERS.put(options.getName(), new Searcher(options, config));
                }

                @Override
                public boolean willOpenStream(String fileName, String fullPath, boolean isDirectory) throws Exception {
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
        switch (s) {
            case "str":
            case "string":
                return SearchFieldType.TYPE_STRING;
            case "int":
                return SearchFieldType.TYPE_INT;
            case "long":
                return SearchFieldType.TYPE_LONG;
            case "float":
                return SearchFieldType.TYPE_FLOAT;
            case "double":
                return SearchFieldType.TYPE_DOUBLE;
            default:
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
