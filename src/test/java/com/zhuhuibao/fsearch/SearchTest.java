package com.zhuhuibao.fsearch;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import com.petkit.base.utils.CollectionUtil;
import com.petkit.base.utils.HttpClient;
import com.petkit.base.utils.JSONUtil;
import com.petkit.base.utils.StringUtil;
import com.zhuhuibao.fsearch.analysis.TokenUtil;
import com.zhuhuibao.fsearch.util.SignUtil;

public class SearchTest extends TestCase {

	private static void request(String api, Map<String, Object> params)
			throws Exception {
		HttpClient client = new HttpClient();
		Map<String, Object> headers = CollectionUtil.arrayAsMap("X-Search-Api",
				"search", "X-Search-Key", "fsearch", "X-Search-Time", "123");
		String secret = "81ac307f7d4547b787aed88f1dc509d6";
		String sign = SignUtil.makeSign(headers, secret);
		headers.put("X-Search-Sign", sign);
		client.doPost("http://localhost:10010/search", params, headers);
		System.out.println(client.getResponseBody());
	}

	public void test_isChineseChar() throws Exception {
		assertTrue(TokenUtil.isChineseChar('华'));
		assertTrue(TokenUtil.isChineseChar('为'));
		assertTrue(!TokenUtil.isChineseChar('，'));
		assertTrue(!TokenUtil.isChineseChar(','));
		assertTrue(!TokenUtil.isChineseChar('H'));
		assertTrue(!TokenUtil.isChineseChar('/'));
	}

	public void test_tokenizeChinese() throws Exception {
		String s = StringUtil.join(TokenUtil.tokenizeChinese("华为HuaWei123我是"),
				",");
		System.out.println(s);
	}
	


	public void testSearch() throws Exception {
		Map<String, Object> query = new HashMap<String, Object>();

		/*query.put("_s",
				CollectionUtil.arrayAsMap("type", "phrase", "value", "摄像头"));
		request("search",
				CollectionUtil.arrayAsMap("table", "product", "query",
						JSONUtil.toJSONString(query)));*/
		query.put("_s",
				CollectionUtil.arrayAsMap("type", "phrase", "value", "屏蔽"));
		request("search",
				CollectionUtil.arrayAsMap("table", "product", "query",
						JSONUtil.toJSONString(query)));
		
		/*Map<String, Object> query2 = new HashMap<String, Object>();
		query2.put("_s",
				CollectionUtil.arrayAsMap("type", "phrase", "value", "国际"));
		request("search",
				CollectionUtil.arrayAsMap("table", "product", "query",
						JSONUtil.toJSONString(query2)));
		Map<String, Object> query3 = new HashMap<String, Object>();
		query3.put("brandid",
				CollectionUtil.arrayAsMap("type", "equal", "value", 4));
		request("search",
				CollectionUtil.arrayAsMap("table", "product", "query",
						JSONUtil.toJSONString(query3)));*/
	}
}
