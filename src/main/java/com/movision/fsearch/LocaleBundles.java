package com.movision.fsearch;

import java.util.Map;

import com.petkit.base.locale.ClassPathPropertiesLocaleBundle;
import com.petkit.base.locale.ClassPathPropertiesLocaleBundle.ClassPathPropertiesLocaleBundleOptions;
import com.petkit.base.locale.LocaleBundle;
import com.petkit.base.utils.FileUtil;

/**
 * 加载conf/locale/error路径下的配置文件,即中文异常配置文件：zh_CN.properties
 */
public class LocaleBundles {
	private static LocaleBundle DEFAULT = null;

    static {
		try {
			ClassPathPropertiesLocaleBundleOptions options = new ClassPathPropertiesLocaleBundleOptions();
			options.setDefaultLocale("zh_CN");
			options.setClazz(LocaleBundles.class);
			options.setPath("conf/locale/error");
			options.setCharset(FileUtil.UTF8);
			DEFAULT = ClassPathPropertiesLocaleBundle.newInstance(options);
		} catch (Exception e) {
			L.error("Failed to load locale files", e);
			System.exit(-1);
		}
	}

	public static void init() {
		// Nope
	}

	public static boolean containsKey(String key) {
		return DEFAULT.containsKey(key);
	}

	public static String get(String locale, String key) {
		return DEFAULT.getWithArrayParams(locale, key, null);
	}

	public static String getWithArrayParams(String locale, String key,
			Object[] params) {
		return DEFAULT.getWithArrayParams(locale, key, params);
	}

	public static String getWithMapParams(String locale, String key,
			Map<String, Object> context) {
		return DEFAULT.getWithMapParams(locale, key, context);
	}
}
