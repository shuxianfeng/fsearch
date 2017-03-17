package com.movision.fsearch.plugins;

import com.movision.fsearch.L;
import com.petkit.base.config.PropertiesConfig;
import com.petkit.base.utils.FileUtil;
import com.movision.fsearch.core.Indexer;
import com.movision.fsearch.core.Searcher;
import com.movision.fsearch.core.SearcherOptions;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import java.io.File;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;


/**
 * 项目信息索引
 */
public class ResumeIndexer implements Indexer {


    @Override
    public void init(SearcherOptions options, PropertiesConfig config) throws Exception {

    }

    @Override
    public Path fullIndex(Searcher searcher) throws Exception {
        SearcherOptions options = searcher.getOptions();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        File dir = new File(options.getPath() + "-"
                + dateFormat.format(new Date()));
        dir.mkdirs();
        Path path = dir.toPath();
        Directory directory = SimpleFSDirectory.open(path);

        try {

            return path;
        } catch (Exception e) {
            FileUtil.delete(path.toFile());
            L.error("执行异常>>>",e);
            throw e;
        } finally {
            FileUtil.close(directory);
        }

    }

    @Override
    public Map<String, Object> parseRawDocument(Map<String, Object> docAsMap) throws Exception {



        return docAsMap;
    }


}

