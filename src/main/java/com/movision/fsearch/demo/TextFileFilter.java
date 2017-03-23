package com.movision.fsearch.demo;

import java.io.File;
import java.io.FileFilter;

/**
 * 此类用于为 .txt 文件过滤器
 *
 * @Author zhuangyuhao
 * @Date 2017/3/22 16:53
 */
public class TextFileFilter implements FileFilter {

    @Override
    public boolean accept(File pathname) {
        return pathname.getName().toLowerCase().endsWith(".txt");
    }
}
