package com.zhuhuibao.fsearch.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.petkit.base.utils.EncryptUtil;

public class SignUtil {
	
	/**
	 * 生成签名
	 * @param kv
	 * @param secret
	 * @return
	 */
	public static String makeSign(Map<?, ?> kv, String secret) {
		Set<?> keySet = kv.keySet();
		List<String> keys = new ArrayList<String>(keySet.size());
		for (Object key : keySet) {
			keys.add(key.toString());
		}
		Collections.sort(keys);

		String encodeString = null;
		{
			StringBuilder buf = new StringBuilder();
			for (String key : keys) {
				Object value = kv.get(key);
				buf.append(key);
				if (value != null) {
					buf.append(value);
				}
			}
			buf.append(secret);
			encodeString = buf.toString();
		}
		String realSign;
		try {
			//md5加密
			realSign = EncryptUtil.md5(encodeString);
		} catch (Exception e) {
			throw new RuntimeException("Failed to encode sign", e);
		}
		return realSign;
	}

	public static boolean checkSign(Map<?, ?> kv, String secret, String sign) {
		if (kv == null || secret == null || sign == null) {
			return false;
		}
		String realSign = makeSign(kv, secret);
		boolean ok = sign.equals(realSign);
		return ok;
	}
}
