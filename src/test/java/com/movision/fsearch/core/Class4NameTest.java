package com.movision.fsearch.core;

import com.petkit.base.utils.FormatUtil;
import junit.framework.TestCase;
import org.apache.lucene.search.SortField;


/**
 * @Author zhuangyuhao
 * @Date 2017/3/23 15:57
 */
public class Class4NameTest extends TestCase {


    public void testClassForName() throws Exception {
        /**
         * Class.forName(xxx)
         * xxx是类的完全限定名，如：com.movision.fsearch.plugins.GoodsIndexer
         */
        Indexer indexer = (Indexer) Class.forName("com.movision.fsearch.plugins.GoodsIndexer").newInstance();
        System.out.println(indexer);
    }

    public void testSortFieldType() {
        SortField.Type type = SortField.Type.valueOf("STRING");
        System.out.println(type);
    }

}