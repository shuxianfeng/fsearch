package com.zhuhuibao.fsearch;

import java.io.InputStream;

import com.petkit.base.config.Config;
import com.petkit.base.config.PropertiesConfig;

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
