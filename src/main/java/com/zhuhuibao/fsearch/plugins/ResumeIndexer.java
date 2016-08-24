package com.zhuhuibao.fsearch.plugins;

import com.petkit.base.config.PropertiesConfig;
import com.petkit.base.repository.db.JdbcTemplate;
import com.petkit.base.repository.db.MapHandler;
import com.petkit.base.utils.FileUtil;
import com.petkit.base.utils.FormatUtil;
import com.zhuhuibao.fsearch.L;
import com.zhuhuibao.fsearch.core.DataSourceManager;
import com.zhuhuibao.fsearch.core.Indexer;
import com.zhuhuibao.fsearch.core.Searcher;
import com.zhuhuibao.fsearch.core.SearcherOptions;
import com.zhuhuibao.fsearch.service.dao.ProjectDao;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;

import java.io.File;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

