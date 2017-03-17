package com.movision.fsearch.core;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

import com.movision.fsearch.L;
import com.movision.fsearch.analysis.TokenUtil;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.SortedDocValuesField;
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
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.search.grouping.GroupingSearch;
import org.apache.lucene.search.grouping.TopGroups;
import org.apache.lucene.search.grouping.GroupDocs;
import org.apache.lucene.util.BytesRef;

import com.chenlb.mmseg4j.MMSeg;
import com.chenlb.mmseg4j.Seg;
import com.chenlb.mmseg4j.Word;
import com.petkit.base.config.PropertiesConfig;
import com.petkit.base.utils.CollectionUtil;
import com.petkit.base.utils.FileUtil;
import com.petkit.base.utils.FormatUtil;
import com.petkit.base.utils.StringUtil;
import com.movision.fsearch.analysis.ComplexAnalyzer;

public class Searcher {
    //实例化中文词法分析器
    public static final Analyzer ANALYZER = new ComplexAnalyzer();
    public static final String SPLIT = ",";
    public static final String TOKEN = " ";
    // private static final Seg[] SEGS = new Seg[] { TokenUtil.getMaxWordSeg(),
    // TokenUtil.getComplexSeg() };

    //这里的IndexWriterConfig，据官方文档介绍，是对indexWriter的配置,传参是词法分析器Analyzer。
    private IndexWriterConfig indexConfig = new IndexWriterConfig(ANALYZER);
    private SearcherOptions options;
    private volatile Path path;
    private Indexer indexer;

    /**
     *
     * @param options
     * @param config
     */
    public Searcher(final SearcherOptions options, final PropertiesConfig config) {
        super();
        this.options = options;
        //搜索选项实体文件
        File dir = new File(options.getPath());
        if (dir.exists()) {
            try {
                //返回抽象路径名的规范路径名字符串
                dir = dir.getCanonicalFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            this.path = dir.toPath();
            //记录日志
            if (L.isInfoEnabled()) {
                L.info("Path for " + options.getName() + " is " + this.path);
            }
        }

        try {
            //实例化索引
            indexer = (Indexer) Class.forName(options.getFullIndexer())
                    .newInstance();
            //初始化索引
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

    /**
     * 通过FSDirectory类加载一个文件目录
     * @return
     * @throws Exception
     */
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
            this.processDocument(doc);
        }
        Directory directory = null;
        IndexWriter writer = null;
        try {
            //打开存储位置
            directory = openDirectory();
            //创建IndexWriter，进行索引文件的写入。
            writer = new IndexWriter(directory, indexConfig);
            //把doc对象加入到索引创建中
            writer.addDocument(doc);
        } finally {
            //关闭IndexWriter,提交创建内容
            close(writer);
            //关闭查询器等
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
        	//通过IndexWriter对象来创建索引
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
            //通过IndexWriter对象来维护索引
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

    /**
     * 分页搜索
     * @param query
     * @param sortFields
     * @param fields
     * @param offset    分页起始值，如第一页0, offset 0 limit 20，第二页 offset 20, limit 20
     * @param limit 每页显示的数目，如20
     * @return
     * @throws Exception
     */
    public Pagination<Map<String, Object>, ProductGroup> searchForPage(Query query,
                                                                       SortField[] sortFields, Collection<String> fields, int offset,
                                                                       int limit) throws Exception {
        /**
         * 1 获取分页的传参：total, offset, limit
         */
        int maxDocs = options.getMaxDocsOfQuery();
        if (offset < 0) {
            offset = 0;
        }
        if (limit <= 0 || limit > maxDocs) {
            limit = 10;
        }
        Sort sort;
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
        IndexSearcher searcher;
        try {
            directory = openDirectory();
            reader = DirectoryReader.open(directory);
            searcher = new IndexSearcher(reader);
            //TopFieldCollector实现索引排序和分页
            //返回命中数
            TopFieldCollector results = TopFieldCollector.create(sort, offset + limit, false, false, false);
            searcher.search(query, results);
            //搜索结果
            ScoreDoc[] hits = results.topDocs(offset, limit).scoreDocs;
            int total = results.getTotalHits();
            if (total <= offset) {
                return Pagination.getEmptyInstance();
            }
            /**
             * 封装items
             */
            List<Map<String, Object>> items = new LinkedList<>();
            for (int i = 0; i < hits.length; i++) {
                Document doc = searcher.doc(hits[i].doc);
                Map<String, Object> item;
                if (fields != null) {
                    item = new HashMap<>(fields.size());
                    for (String fieldName : fields) {
                        item.put(fieldName,
                                deserializeFieldValue(doc.getField(fieldName)));
                    }
                } else {
                    List<IndexableField> iFields = doc.getFields();
                    item = new HashMap<>(iFields.size());
                    for (IndexableField iField : iFields) {
                        item.put(iField.name(), deserializeFieldValue(iField));
                    }
                }
                items.add(item);
            }
            // TODO: 2017/3/16  对产品搜索特殊处理
            /**
             * 封装productGroups
             */
            if (options.getName().equals("product")) {
                List<ProductGroup> productGroups = new ArrayList<>();
                if (!items.isEmpty()) {

                    ProductGroup scateGroup = groupByField(searcher, query, "scateid1");
                    if (null != scateGroup) {
                        productGroups.add(scateGroup);
                    }
                    ProductGroup brandGroup = groupByField(searcher, query, "brandid1");
                    if (null != brandGroup) {
                        productGroups.add(brandGroup);
                    }
                }
                return new Pagination<>(items, productGroups, total, offset, limit);
            } else {
                return new Pagination<>(items, null, total, offset, limit);
            }

        } finally {
            close(reader);
            close(directory);
        }
    }


    /**
     *  根据groupField封装成ProductGroup
     * @param searcher
     * @param query
     * @param groupField 要分组的字段的名称
     * @return
     * @throws Exception
     */
    public ProductGroup groupByField(IndexSearcher searcher, Query query, String groupField)
            throws Exception {
        if (null == searcher || null == query || null == groupField) {
            return null;
        }
        GroupingSearch groupingSearch = new GroupingSearch(groupField);
        Sort groupSort = Sort.RELEVANCE;
        groupingSearch.setGroupSort(groupSort); //指定分组排序
        groupingSearch.setFillSortFields(true);     //是否也填写每个返回组和组文档的排序字段
        groupingSearch.setCachingInMB(4.0, true);   //缓存第一次搜索结果，用于第二次搜索
        groupingSearch.setAllGroups(true);  //是否还计算与查询匹配的所有组
        //groupingSearch.setAllGroupHeads(true);
        groupingSearch.setGroupDocsLimit(10);   //指定每组返回的文档数

        TopGroups<BytesRef> result = groupingSearch.search(searcher, query, 0, searcher.getIndexReader().maxDoc());
        if (null == result || 0 == result.groups.length) {
            return null;
        }

        ProductGroup productGroup = new ProductGroup();

        if (groupField.equals("scateid1")) {
            productGroup.setKey("scateid");
            productGroup.setName("产品分类");

        } else if (groupField.equals("brandid1")) {
            productGroup.setKey("brandid");
            productGroup.setName("品牌");
        }


        /**
         *  下面是封装List<GroupValue>
         */
        int groupItemCount = options.getGroupItemCount();
        int index = 0;
        List<GroupValue> values = new ArrayList<>();
        //遍历搜索结果分组
        //GroupDocs ： 表示结果中的一个组。
        for (GroupDocs<BytesRef> groupDocs : result.groups) {
            index++;
            if (index <= groupItemCount) {
                //分组名称
                String groupId = groupDocs.groupValue.utf8ToString();
                //分组中的文档
                Document doc = searcher.doc(groupDocs.scoreDocs[0].doc);

                String groupName = "其它";
                if (groupField.equals("scateid1")) {
                    groupName = doc.get("scate_name");
                } else if (groupField.equals("brandid1")) {
                    groupName = doc.get("brand_CNName");
                }
                values.add(new GroupValue(groupId, groupName));
            } else {
                break;
            }
        }
        productGroup.setValues(values);

        return productGroup;
    }

    public Document parseDocument(Map<String, Object> docAsMap) {
        boolean test=false;
        Document doc = new Document();
        // StringBuilder generalTokenized = options.getGeneralSearchField() ==
        // null ? null
        // : new StringBuilder();
        List<String> tokens = null;
        Set<String> tokenSet = null;
        if (options.getGeneralSearchField() != null) {
            tokens = new LinkedList<>();
            tokenSet = new HashSet<>();
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
            boolean group = false;
            if (field == null) {
                if (value instanceof Map) {
                    Map<?, ?> valueAsMap = (Map<?, ?>) value;
                    strValue = FormatUtil.parseString(valueAsMap.get("value"));
                    store = FormatUtil.parseBoolean(valueAsMap.get("store"),store);
                    index = FormatUtil.parseBoolean(valueAsMap.get("index"),index);
                    tokenized = FormatUtil.parseBoolean(valueAsMap.get("tokenized"), tokenized);
                    generalSearch = FormatUtil.parseBoolean(valueAsMap.get("generalSearch"), generalSearch);
                }
            } else {
                switch (field.getType()) {
                    case SearchFieldType.TYPE_STRING: {
                        strValue = value.toString();
                        store = field.isStore();
                        index = field.isIndex();
                        tokenized = field.isTokenized();
                        generalSearch = field.isGeneralSearch();
                        group = field.isGroup();
                        break;
                    }
                    case SearchFieldType.TYPE_INT: {
                        if (!field.isGroup()) {
                            f = new IntField(key, FormatUtil.parseInteger(value),
                                    numberFieldType(
                                            field.isStore() ? IntField.TYPE_STORED
                                                    : IntField.TYPE_NOT_STORED,
                                            field.isIndex()));
                        } else {
                            f = new SortedDocValuesField(key, new BytesRef(value.toString()));
                        }
                        break;
                    }
                    case SearchFieldType.TYPE_LONG: {
                        if (!field.isGroup()) {
                            f = new LongField(key, FormatUtil.parseLong(value),
                                    numberFieldType(
                                            field.isStore() ? LongField.TYPE_STORED
                                                    : LongField.TYPE_NOT_STORED,
                                            field.isIndex()));
                        } else {
                            f = new SortedDocValuesField(key, new BytesRef(value.toString()));
                        }
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
                if (!group) {
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
                } else {
                    f = new SortedDocValuesField(key, new BytesRef(strValue));
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
            Seg[] segs = new Seg[]{TokenUtil.getMaxWordSeg(),
                    TokenUtil.getComplexSeg()};
            for (Seg seg : segs) {
                MMSeg mmSeg = new MMSeg(new StringReader(s), seg);
                Word word;
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
