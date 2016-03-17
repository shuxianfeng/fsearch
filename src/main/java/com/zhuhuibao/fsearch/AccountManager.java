package com.zhuhuibao.fsearch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.petkit.base.utils.JSONUtil;

public class AccountManager {
	private static Map<String, String> ACCOUNTS = null;
	static {
		try {
			List<?> accounts = JSONUtil.parseAsList(G.getConfig().getString(
					"accounts"));
			ACCOUNTS = new HashMap<String, String>(accounts.size());
			for (Object account : accounts) {
				Map<?, ?> accountAsMap = (Map<?, ?>) account;
				ACCOUNTS.put(accountAsMap.get("key").toString(), accountAsMap
						.get("secret").toString());
			}
		} catch (Exception e) {
			L.error("Failed to load AccountManager", e);
			System.exit(-1);
		}
	}

	public static void init() {
		// Nope
	}

	public static String findSecret(String id) {
		return ACCOUNTS.get(id);
	}
}
