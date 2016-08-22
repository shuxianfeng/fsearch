package com.zhuhuibao.fsearch;

import java.text.SimpleDateFormat;
import java.util.*;

import com.petkit.base.utils.*;
import junit.framework.TestCase;

import com.zhuhuibao.fsearch.analysis.TokenUtil;
import com.zhuhuibao.fsearch.util.SignUtil;
import org.apache.lucene.document.DateTools;

public class SearchTest extends TestCase {

    private static void request(String api, Map<String, Object> params)
            throws Exception {
        HttpClient client = new HttpClient();
        Map<String, Object> headers = CollectionUtil.arrayAsMap("X-Search-Api",
                api, "X-Search-Key", "fsearch", "X-Search-Time", "123");
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

    public void testProjectSearch() throws Exception {
        Map<String, Object> query = new HashMap<>();

//        query.put("city",
//                CollectionUtil.arrayAsMap("type", "equal", "value", "110100"));

        String startDate = "2016-05-02";
        String endDate = "2017-07-02";

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date sdate = sdf.parse(startDate);
        startDate = DateTools.dateToString(sdate, DateTools.Resolution.DAY);
        long min= Long.valueOf(startDate);

        Date edate = sdf.parse(endDate);
        endDate = DateTools.dateToString(edate, DateTools.Resolution.DAY);
        long max= Long.valueOf(endDate);

        query.put("startDate",
                CollectionUtil.arrayAsMap(
                        "type","numberrange",
                        "maxInclusive","true",
                        "minInclusive","true",
                        "max",max,
                        "min",min));


        List<Map<String, Object>> sortFields = new ArrayList<>(1);
        Map<String,Object> sort = new HashMap<>();
        sort.put("field","updateDate");
        sort.put("reverse",true);
        sort.put("type","INT");
        sortFields.add(sort);
        Map<String, Object> params = CollectionUtil.arrayAsMap(
                "table", "project",
                "query",JSONUtil.toJSONString(query),
                "sort",JSONUtil.toJSONString(sortFields),
                "offset",0,
                "limit", 10);

        request("search", params);

    }


    public void testSearch() throws Exception {
        Map<String, Object> query = new HashMap<String, Object>();

		/*query.put("_s",
                CollectionUtil.arrayAsMap("type", "phrase", "value", "摄像头"));
		request("search",
				CollectionUtil.arrayAsMap("table", "product", "query",
						JSONUtil.toJSONString(query)));*/
		/*query.put("_s",
				CollectionUtil.arrayAsMap("type", "phrase", "value", "华为"));
		request("search",
				CollectionUtil.arrayAsMap("table", "product", "query",
						JSONUtil.toJSONString(query)));*/
		
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

        query.put("_s",
                CollectionUtil.arrayAsMap("type", "phrase", "value", "东大智能"));
        List<Map<String,Object>> sortFields = new ArrayList<>();

        Map<String,Object> sortField = new HashMap<>();
        sortField.put("field", "certLevel");
        sortField.put("type", "LONG");
        sortField.put("reverse",FormatUtil.parseBoolean("true"));
        sortFields.add(sortField);

        request("search",
                CollectionUtil.arrayAsMap("table", "contractor",
                        "query",JSONUtil.toJSONString(query),
                        "sort",JSONUtil.toJSONString(sortFields),
                        "offset","0",
                        "limit","70"));
       /*
        Map<String, Object> query2 = new HashMap<String, Object>();
        query2.put("province",
                CollectionUtil.arrayAsMap("type", "equal", "value", "120000"));
        request("search",
                CollectionUtil.arrayAsMap("table", "contractor", "query",
                        JSONUtil.toJSONString(query2)));

        Map<String, Object> query3 = new HashMap<String, Object>();
        query3.put("_s",
                CollectionUtil.arrayAsMap("type", "phrase", "value", "海康"));
        request("search",
                CollectionUtil.arrayAsMap("table", "product", "query",
                        JSONUtil.toJSONString(query3)));

        Map<String, Object> query4 = new HashMap<String, Object>();
        query4.put("涉及国家秘密的计算机信息系统集成甲级",
                CollectionUtil.arrayAsMap("type", "equal", "value", "涉及国家秘密的计算机信息系统集成甲级"));
        request("search",
                CollectionUtil.arrayAsMap("table", "contractor", "query",
                        JSONUtil.toJSONString(query4)));

        Map<String, Object> query5 = new HashMap<String, Object>();
        query5.put("4",
                CollectionUtil.arrayAsMap("type", "equal", "value", "4"));
        request("search",
                CollectionUtil.arrayAsMap("table", "supplier", "query",
                        JSONUtil.toJSONString(query5)));

        Map<String, Object> query6 = new HashMap<String, Object>();
        query6.put("守合同重信用企业",
                CollectionUtil.arrayAsMap("type", "equal", "value", "南京"));
        request("search",
                CollectionUtil.arrayAsMap("table", "supplier", "query",
                        JSONUtil.toJSONString(query6)));
          */
    }
}
