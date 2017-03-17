package com.movision.fsearch;

import java.io.InputStream;

import com.petkit.base.config.Config;
import com.petkit.base.config.PropertiesConfig;

/**
 * 搜索引擎初始化配置文件 加载器
 */
public class G {
	private static final PropertiesConfig config = new PropertiesConfig(
			getResourceAsStream("conf/config.ini"), false);

	public static Config getConfig() {
		return config;
	}

	public static InputStream getResourceAsStream(String name) {
		return G.class.getResourceAsStream(name);
	}
}
