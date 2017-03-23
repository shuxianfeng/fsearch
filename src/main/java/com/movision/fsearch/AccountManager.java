package com.movision.fsearch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.petkit.base.utils.JSONUtil;

/**
 * 账户管理类，获取当前账号
 */
public class AccountManager {

	private static Map<String, String> ACCOUNTS = null;

	static {
		try {
			//获取当前账号
			//accounts = [{"key":"fsearch","secret":"81ac307f7d4547b787aed88f1dc509d6"}]
			List<?> accounts = JSONUtil.parseAsList(G.getConfig().getString(
					"accounts"));
			ACCOUNTS = new HashMap<String, String>(accounts.size());
			for (Object account : accounts) {
				Map<?, ?> accountAsMap = (Map<?, ?>) account;

                ACCOUNTS.put(accountAsMap.get("key").toString(), accountAsMap.get("secret").toString());
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
