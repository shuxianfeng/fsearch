package com.movision.fsearch;

import java.io.InputStream;

import com.petkit.base.utils.FileUtil;
import com.petkit.base.utils.FileUtil.FileListHandler;

public class FileUtilTest {
	public static void main(String[] args) throws Exception {

		// FileUtil.listClassPathFiles(
		// "/opt/rd/projects/fsearch/target/lib/fsearch.jar",
		// "/com/zhuhuibao/fsearch/conf/", new FileListHandler() {
		//
		// @Override
		// public boolean willOpenStream(String fileName,
		// String fullPath, boolean isDirectory)
		// throws Exception {
		// System.out.println("willOpenStream: " + fileName
		// + ", fullPath: " + fullPath);
		// return true;
		// }
		//
		// @Override
		// public void streamOpened(String fileName, String fullPath,
		// InputStream in) throws Exception {
		// System.out.println("streamOpened: " + fileName
		// + ", fullPath: " + fullPath);
		// }
		//
		// });

		FileUtil.listClassPathFiles(LocaleBundles.class, "conf/",
				new FileListHandler() {

					@Override
					public boolean willOpenStream(String fileName,
							String fullPath, boolean isDirectory)
							throws Exception {
						System.out.println("willOpenStream: " + fileName
								+ ", fullPath: " + fullPath);
						return true;
					}

					@Override
					public void streamOpened(String fileName, String fullPath,
							InputStream in) throws Exception {
						System.out.println("streamOpened: " + fileName
								+ ", fullPath: " + fullPath);
					}

				});
	}
}
