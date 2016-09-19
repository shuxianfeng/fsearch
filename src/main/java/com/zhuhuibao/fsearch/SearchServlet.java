package com.zhuhuibao.fsearch;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;

import com.petkit.base.utils.CollectionUtil;
import com.petkit.base.utils.FormatUtil;
import com.petkit.base.utils.JSONUtil;
import com.petkit.base.utils.StringUtil;
import com.zhuhuibao.fsearch.core.QueryUtil;
import com.zhuhuibao.fsearch.core.SearchManager;
import com.zhuhuibao.fsearch.core.Searcher;
import com.zhuhuibao.fsearch.exception.ApiException;
import com.zhuhuibao.fsearch.exception.ArgumentApiException;
import com.zhuhuibao.fsearch.exception.UnknownApiException;
import com.zhuhuibao.fsearch.util.SignUtil;

public class SearchServlet extends HttpServlet {
	private static final String KEY_API = "X-Search-Api";
	private static final String KEY_KEY = "X-Search-Key";
	private static final String KEY_TIME = "X-Search-Time";
	private static final String KEY_SIGN = "X-Search-Sign";
	private static final String[] KEYS = new String[] { KEY_API, KEY_KEY,
			KEY_TIME, KEY_SIGN };
	static {
		LocaleBundles.init();
		AccountManager.init();
		SearchManager.init();
	}

	public SearchServlet() {
	}

	private static boolean outputJSON(HttpServletResponse response, int status,
			String s) {
		try {
			response.setStatus(status);
			response.setContentType("application/json;charset=utf-8");
			response.setHeader("Cache-Control", "no-cache");
			response.getWriter().write(s);
			response.getWriter().close();
			return true;
		} catch (Exception ex) {
			L.error(ex);
			return false;
		}
	}

	private Object handle(HttpServletRequest request) throws Exception {
		Map<String, String> kv = new HashMap<String, String>(KEYS.length);
		for (String key : KEYS) {
			String value = request.getHeader(key);
			if (value == null || value.isEmpty()) {
				throw new ArgumentApiException(key);
			}
			kv.put(key, value);
		}
		String secret = AccountManager.findSecret(kv.get(KEY_KEY));
		if (secret == null) {
			throw new ArgumentApiException(KEY_KEY);
		}
		String sign = kv.remove(KEY_SIGN);
		if (sign == null) {
			throw new ArgumentApiException(KEY_SIGN);
		}
		boolean ok = SignUtil.checkSign(kv, secret, sign);
		if (!ok) {
			throw new ArgumentApiException(KEY_SIGN);
		}
		String api = kv.get(KEY_API);
		switch (api) {
			case "search":
				return search(request);
			case "document_add":
				return saveDocument(request, false);
			case "document_update":
				return saveDocument(request, true);
			case "document_remove":
				return removeDocument(request);
			default:
				throw new ArgumentApiException(KEY_API);
		}
	}

	private Object saveDocument(HttpServletRequest request, boolean update)
			throws Exception {
		String table = request.getParameter("table");
		String docAsStr = request.getParameter("doc");
		Map<String, Object> docAsMap = JSONUtil.parseAsMap(docAsStr);
		Searcher searcher = SearchManager.getSearcher(table);
		docAsMap = searcher.getIndexer().parseRawDocument(docAsMap);
		Document doc = searcher.parseDocument(docAsMap);
		if (update) {
			Object id = docAsMap.get(searcher.getOptions().getIdField());
			if (id == null) {
				throw new ArgumentApiException("id");
			}
			searcher.updateDocument(FormatUtil.parseString(id), doc);
		} else {
			searcher.addDocument(doc);
		}
		return null;
	}

	private Object removeDocument(HttpServletRequest request) throws Exception {
		String table = request.getParameter("table");
		String id = request.getParameter("id");
		if (StringUtil.isEmpty(id)) {
			throw new ArgumentApiException("id");
		}
		Searcher searcher = SearchManager.getSearcher(table);
		searcher.removeDocument(id);
		return null;
	}

	private Object search(HttpServletRequest request) throws Exception {
		String table = request.getParameter("table");
		Map<String, Object> queryAsMap = JSONUtil.parseAsMap(request
				.getParameter("query"));
		int offset = FormatUtil
				.parseIntValue(request.getParameter("offset"), 0);
		int limit = FormatUtil.parseIntValue(request.getParameter("limit"), 0);
		List<?> sortAsList = JSONUtil.parseAsList(request.getParameter("sort"));
		List<String> fields = StringUtil.split(request.getParameter("fields"),
				",");
		Searcher searcher = SearchManager.getSearcher(table);
		Query query = QueryUtil.parseQuery(searcher, queryAsMap);
		SortField[] sort = QueryUtil.parseSort(sortAsList);
		
		L.warn("SearchServlet:search:"+queryAsMap.toString()+"===="+query.toString()+"===="+sort.toString());
		return searcher.searchForPage(query, sort, fields, offset, limit);
	}

	private void outputApiResult(HttpServletResponse response, Throwable t,
			Object ret) {
		Object result = null;
		if (t != null) {
			ApiException se = null;
			if (t instanceof ApiException) {
				se = (ApiException) t;
			} else {
				se = new UnknownApiException(t);
			}
			String errorMsg = LocaleBundles.getWithArrayParams(null,
					se.getErrorCode(), se.getErrorParams());
			result = CollectionUtil.arrayAsMap("error", CollectionUtil
					.arrayAsMap("code", se.getErrorCode(), "msg", errorMsg,
							"data", se.getErrorData()));
		} else {
			if (ret == null) {
				ret = "success";
			}
			result = CollectionUtil.arrayAsMap("result", ret);
		}
		String resultAsJSONStr = JSONUtil.toJSONString(result);
		outputJSON(response, HttpServletResponse.SC_OK, resultAsJSONStr);
	}

	private void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		try {
			Object ret = handle(request);
			outputApiResult(response, null, ret);
		} catch (Throwable e) {
			L.error(e);
			outputApiResult(response, e, null);
		}
	}

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		handleRequest(request, response);
	}

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		handleRequest(request, response);
	}
}
