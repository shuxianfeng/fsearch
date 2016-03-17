package com.zhuhuibao.fsearch.core;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import com.chenlb.mmseg4j.MMSeg;
import com.chenlb.mmseg4j.Seg;
import com.chenlb.mmseg4j.Word;
import com.petkit.base.config.PropertiesConfig;
import com.petkit.base.utils.CollectionUtil;
import com.petkit.base.utils.FileUtil;
import com.petkit.base.utils.FormatUtil;
import com.petkit.base.utils.StringUtil;
import com.zhuhuibao.fsearch.L;
import com.zhuhuibao.fsearch.analysis.ComplexAnalyzer;
import com.zhuhuibao.fsearch.analysis.TokenUtil;

public class Searcher {
	public static final Analyzer ANALYZER = new ComplexAnalyzer();
	public static final String SPLIT = ",";
	public static final String TOKEN = " ";
	// private static final Seg[] SEGS = new Seg[] { TokenUtil.getMaxWordSeg(),
	// TokenUtil.getComplexSeg() };

	private IndexWriterConfig indexConfig = new IndexWriterConfig(ANALYZER);
	private SearcherOptions options;
	private volatile Path path;
	private Indexer indexer;

	public Searcher(final SearcherOptions options, final PropertiesConfig config) {
		super();
		this.options = options;
		File dir = new File(options.getPath());
		if (dir.exists()) {
			try {
				dir = dir.getCanonicalFile();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			this.path = dir.toPath();
			if (L.isInfoEnabled()) {
				L.info("Path for " + options.getName() + " is " + this.path);
			}
		}

		try {
			indexer = (Indexer) Class.forName(options.getFullIndexer())
					.newInstance();
			indexer.init(options, config);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		Timer timer = new Timer();
		long interval = 1000L * ((long) options.getFullIndexInterval());
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				try {
					buildFullIndexes();
				} catch (Throwable t) {
					L.error(t);
				}
			}

		}, options.isFullIndexAtStart() ? 0 : interval, interval);
	}

	public Indexer getIndexer() {
		return indexer;
	}

	private void buildFullIndexes() {
		try {
			long t1 = System.currentTimeMillis();
			if (L.isInfoEnabled()) {
				L.info("FullIndex start: " + options.getFullIndexer());
			}
			Path newPath = indexer.fullIndex(this);
			long t2 = System.currentTimeMillis();
			if (L.isInfoEnabled()) {
				L.info("FullIndex complete: " + options.getFullIndexer()
						+ ",using: " + (t2 - t1) / 1000L + "s");
			}
			path = newPath;
		} catch (Throwable ex) {
			L.error("FullIndex failed: " + options.getFullIndexer(), ex);
			return;
		}
		// Ensure no request on old path
		try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			L.error(e);
		}
		// Make new index file take effect
		try {
			File symbolicLink = new File(options.getPath());
			if (symbolicLink.exists()) {
				File canonicalFile = symbolicLink.getCanonicalFile();
				FileUtil.delete(symbolicLink);
				FileUtil.delete(canonicalFile);
			}
			Files.createSymbolicLink(symbolicLink.toPath(), path);
		} catch (IOException e) {
			L.error(e);
		}
	}

	public Directory openDirectory() throws Exception {
		if (path == null) {
			throw new RuntimeException("No path set");
		}
		return SimpleFSDirectory.open(path);
	}

	private void close(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {

			}
		}
	}

	private Object deserializeFieldValue(IndexableField field) {
		SearchField fieldDef = options.findField(field.name());
		if (fieldDef == null
				|| fieldDef.getType() == SearchFieldType.TYPE_STRING) {
			return field.stringValue();
		}
		return field.numericValue();
	}

	private void processDocument(Document doc) {
		if (doc.getField(options.getIdField()) == null) {
			throw new IllegalArgumentException("No id set");
		}
	}

	public void addDocument(Document doc) throws Exception {
		if (doc.getField(options.getIdField()) == null) {
			processDocument(doc);
		}
		Directory directory = null;
		IndexWriter writer = null;
		try {
			directory = openDirectory();
			writer = new IndexWriter(directory, indexConfig);
			writer.addDocument(doc);
		} finally {
			close(writer);
			close(directory);
		}
	}

	public void addDocuments(Collection<Document> docs) throws Exception {
		addDocuments(docs, openDirectory(), true);
	}

	public void addDocuments(Collection<Document> docs, Directory directory,
			boolean closeDirectory) throws Exception {
		for (Document doc : docs) {
			processDocument(doc);
		}
		IndexWriter writer = null;
		try {
			writer = new IndexWriter(directory, indexConfig);
			writer.addDocuments(docs);
		} finally {
			close(writer);
			if (closeDirectory) {
				close(directory);
			}
		}
	}

	public void updateDocument(String id, Document doc) throws Exception {
		processDocument(doc);
		Directory directory = null;
		IndexWriter writer = null;
		try {
			directory = openDirectory();
			writer = new IndexWriter(directory, indexConfig);
			writer.updateDocument(new Term(options.getIdField(), id), doc);
		} finally {
			close(writer);
			close(directory);
		}
	}

	public void removeDocument(String id) throws Exception {
		Directory directory = null;
		IndexWriter writer = null;
		try {
			directory = openDirectory();
			writer = new IndexWriter(directory, indexConfig);
			writer.deleteDocuments(new Term(options.getIdField(), id));
		} finally {
			close(writer);
			close(directory);
		}
	}

	public Pagination<Map<String, Object>> searchForPage(Query query,
			SortField[] sortFields, Collection<String> fields, int offset,
			int limit) throws Exception {
		int maxDocs = options.getMaxDocsOfQuery();
		if (offset < 0) {
			offset = 0;
		} else if (offset >= maxDocs) {
			return Pagination.getEmptyInstance();
		}
		if (limit <= 0 || limit > maxDocs) {
			limit = 10;
		}
		Sort sort = null;
		if (sortFields != null) {
			sort = new Sort(sortFields);
		} else {
			sort = new Sort(SortField.FIELD_SCORE);
		}
		if (L.isInfoEnabled()) {
			L.info("searchForPage, query: " + query + ", sort: "
					+ StringUtil.join(sort.getSort()) + ", fields: "
					+ StringUtil.join(fields) + ", offset: " + offset
					+ ", limit: " + limit);
		}

		Directory directory = null;
		DirectoryReader reader = null;
		IndexSearcher searcher = null;
		try {
			directory = openDirectory();
			reader = DirectoryReader.open(directory);
			searcher = new IndexSearcher(reader);
			int total = searcher.count(query);
			if (total <= offset) {
				return Pagination.getEmptyInstance();
			}

			ScoreDoc[] hits = searcher.search(query, offset + limit, sort,
					false, false).scoreDocs;
			List<Map<String, Object>> items = new LinkedList<Map<String, Object>>();
			for (int i = offset; i < hits.length; i++) {
				Document doc = searcher.doc(hits[i].doc);
				Map<String, Object> item = null;
				if (fields != null) {
					item = new HashMap<String, Object>(fields.size());
					for (String fieldName : fields) {
						item.put(fieldName,
								deserializeFieldValue(doc.getField(fieldName)));
					}
				} else {
					List<IndexableField> iFields = doc.getFields();
					item = new HashMap<String, Object>(iFields.size());
					for (IndexableField iField : iFields) {
						item.put(iField.name(), deserializeFieldValue(iField));
					}
				}
				items.add(item);
			}
			return new Pagination<Map<String, Object>>(items, total, offset,
					limit);
		} finally {
			close(reader);
			close(directory);
		}
	}

	public Document parseDocument(Map<String, Object> docAsMap) {
		Document doc = new Document();
		// StringBuilder generalTokenized = options.getGeneralSearchField() ==
		// null ? null
		// : new StringBuilder();
		List<String> tokens = null;
		Set<String> tokenSet = null;
		if (options.getGeneralSearchField() != null) {
			tokens = new LinkedList<String>();
			tokenSet = new HashSet<String>();
		}

		for (Entry<String, Object> entry : docAsMap.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value == null) {
				continue;
			}
			SearchField field = options.findField(key);
			Field f = null;
			String strValue = null;
			boolean store = false;
			boolean index = false;
			boolean generalSearch = false;
			boolean tokenized = false;
			if (field == null) {
				if (value instanceof Map) {
					Map<?, ?> valueAsMap = (Map<?, ?>) value;
					strValue = FormatUtil.parseString(valueAsMap.get("value"));
					store = FormatUtil.parseBoolean(valueAsMap.get("store"),
							store);
					index = FormatUtil.parseBoolean(valueAsMap.get("index"),
							index);
					tokenized = FormatUtil.parseBoolean(
							valueAsMap.get("tokenized"), tokenized);
					generalSearch = FormatUtil.parseBoolean(
							valueAsMap.get("generalSearch"), generalSearch);
				}
			} else {
				switch (field.getType()) {
				case SearchFieldType.TYPE_STRING: {
					strValue = value.toString();
					store = field.isStore();
					index = field.isIndex();
					tokenized = field.isTokenized();
					generalSearch = field.isGeneralSearch();
					break;
				}
				case SearchFieldType.TYPE_INT: {
					f = new IntField(key, FormatUtil.parseInteger(value),
							numberFieldType(
									field.isStore() ? IntField.TYPE_STORED
											: IntField.TYPE_NOT_STORED,
									field.isIndex()));
					break;
				}
				case SearchFieldType.TYPE_LONG: {
					f = new LongField(key, FormatUtil.parseLong(value),
							numberFieldType(
									field.isStore() ? LongField.TYPE_STORED
											: LongField.TYPE_NOT_STORED,
									field.isIndex()));
					break;
				}
				case SearchFieldType.TYPE_FLOAT: {
					f = new FloatField(key, FormatUtil.parseFloat(value),
							numberFieldType(
									field.isStore() ? FloatField.TYPE_STORED
											: FloatField.TYPE_NOT_STORED,
									field.isIndex()));
					break;
				}
				case SearchFieldType.TYPE_DOUBLE: {
					f = new DoubleField(key, FormatUtil.parseDouble(value),
							numberFieldType(
									field.isStore() ? DoubleField.TYPE_STORED
											: DoubleField.TYPE_NOT_STORED,
									field.isIndex()));
					break;
				}
				default:
					throw new RuntimeException("Bad field type: "
							+ field.getType());
				}
			}
			if (StringUtil.isNotEmpty(strValue)) {
				if (generalSearch && tokens != null) {
					// generalTokenized.append(key).append(":");
					seg(strValue, tokenSet, tokens);
				}
				if (index || store) {
					FieldType fType = new FieldType();
					fType.setStored(store);
					if (index) {
						fType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
						fType.setTokenized(tokenized);
					} else {
						fType.setIndexOptions(IndexOptions.NONE);
					}
					f = new Field(key, strValue, fType);
				}
			}
			if (f == null) {
				continue;
			}
			doc.add(f);
		}

		if (CollectionUtil.isNotEmpty(tokens)) {
			String searchString = StringUtil.join(tokens, TOKEN);
			FieldType fType = new FieldType();
			fType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
			fType.setStored(false);
			fType.setTokenized(true);
			doc.add(new Field(options.getGeneralSearchField(), searchString,
					fType));
		}
		return doc;
	}

	public SearcherOptions getOptions() {
		return options;
	}

	private static FieldType numberFieldType(FieldType raw, boolean index) {
		FieldType newType = new FieldType(raw);
		if (index) {
			newType.setDocValuesType(DocValuesType.NUMERIC);
			newType.setTokenized(false);
		} else {
			newType.setIndexOptions(IndexOptions.NONE);
		}
		newType.freeze();
		return newType;
	}

	private static void seg(String s, Set<String> tokenSet, List<String> tokens) {
		try {
			Seg[] segs = new Seg[] { TokenUtil.getMaxWordSeg(),
					TokenUtil.getComplexSeg() };
			for (Seg seg : segs) {
				MMSeg mmSeg = new MMSeg(new StringReader(s), seg);
				Word word = null;
				while ((word = mmSeg.next()) != null) {
					String smallWord = word.getString();
					if (!tokenSet.contains(smallWord)) {
						tokenSet.add(smallWord);
						tokens.add(smallWord);
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
}
