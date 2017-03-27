package com.movision.fsearch.core;

import java.nio.file.Path;
import java.util.Map;

import com.petkit.base.config.PropertiesConfig;

public interface Indexer {

    /**
     * 延时0毫秒后重复的执行task(即更新字典)，周期是30分钟
     *
     * @param options
     * @param config
     * @throws Exception
     */
    void init(SearcherOptions options, PropertiesConfig config) throws Exception;

    /**
     * 完整的索引过程
     * 根据搜索结果创建索引（把Documents添加到指定的Directory的索引中）
     * @param searcher
     * @return
     * @throws Exception
     */
	Path fullIndex(Searcher searcher) throws Exception;

    /**
     * 解析搜索结果的一条记录，并在其中添加需要的键值对
     * @param doc
     * @return
     * @throws Exception
     */
	Map<String, Object> parseRawDocument(Map<String, Object> doc)
			throws Exception;

}
