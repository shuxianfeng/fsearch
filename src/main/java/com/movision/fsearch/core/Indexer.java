package com.movision.fsearch.core;

import java.nio.file.Path;
import java.util.Map;

import com.petkit.base.config.PropertiesConfig;

public interface Indexer {

	void init(SearcherOptions options, PropertiesConfig config) throws Exception;

	Path fullIndex(Searcher searcher) throws Exception;

	Map<String, Object> parseRawDocument(Map<String, Object> doc)
			throws Exception;

}
