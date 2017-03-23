package com.movision.fsearch.demo;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * @Author zhuangyuhao
 * @Date 2017/3/22 17:19
 */
public class Searcher {
    IndexSearcher indexSearcher;
    QueryParser queryParser;
    Query query;

    public Searcher(String indexDirectoryPath)
            throws IOException {

        File dir = new File(indexDirectoryPath);
        Path path = dir.toPath();
        Directory indexDirectory = FSDirectory.open(path);

        DirectoryReader reader = null;
        reader = DirectoryReader.open(indexDirectory);
        indexSearcher = new IndexSearcher(reader);


        queryParser = new QueryParser(Version.LUCENE_5_1_0.toString(), com.movision.fsearch.core.Searcher.ANALYZER);
    }

    public TopDocs search(String searchQuery)
            throws IOException, ParseException {
        query = queryParser.parse(searchQuery);
        return indexSearcher.search(query, LuceneConstants.MAX_SEARCH);
    }

    public Document getDocument(ScoreDoc scoreDoc)
            throws CorruptIndexException, IOException {
        return indexSearcher.doc(scoreDoc.doc);
    }

//    public void close() throws IOException {
//        indexSearcher.();
//    }
}
