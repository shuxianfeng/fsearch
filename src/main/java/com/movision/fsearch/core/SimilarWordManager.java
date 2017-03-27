package com.movision.fsearch.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.movision.fsearch.L;
import com.movision.fsearch.analysis.TokenUtil;
import com.petkit.base.repository.RepositoryException;
import com.petkit.base.repository.db.JdbcTemplate;
import com.petkit.base.repository.db.MapHandler;
import com.petkit.base.repository.db.StringPropertyHandler;
import com.petkit.base.utils.StringUtil;

public class SimilarWordManager {

	private static Map<String, List<String>> SIMILAR_WORD_MAP = new HashMap<String, List<String>>(
			0);

    /**
     * 在t_goods_words_similar表中查找所有的相似词，并放入SIMILAR_WORD_MAP中。
     */
    static {
		List<Map<String, Object>> items;
		try {
            //t_goods_words_similar
            items = DataSourceManager.getJdbcTemplate().findList(
                    "select w,s from t_goods_words_similar", null, 0, 0,
                    MapHandler.CASESENSITIVE);
			Map<String, List<String>> similarMap = new HashMap<String, List<String>>(
					0);
			for (Map<String, Object> item : items) {
				String w = item.get("w").toString();
				String s = item.get("s").toString();
				List<String> words = StringUtil.split(s, ",");
				words.add(w);
				for (String word : words) {
					similarMap.put(word, words);
				}
			}
			SIMILAR_WORD_MAP = similarMap;
		} catch (RepositoryException e) {
			L.error("Failed to init SimilarWordManager", e);
			System.exit(-1);
		}
	}

    /**
     * 在SIMILAR_WORD_MAP中寻找相似的词
     *
     * @param w
     * @return
     * @throws RepositoryException
     */
    public static List<String> findSimilarWords(String w)
			throws RepositoryException {
		if (w == null || w.length() == 0) {
			return null;
		}
		if (!TokenUtil.isChineseChar(w.charAt(0))) {
			return null;
		}
		return SIMILAR_WORD_MAP.get(w);
    }

    /**
     * 新增t_goods_words_similar中的相似词
     *
     * @param w1
     * @param w2
     * @throws RepositoryException
     */
	public static void create(String w1, String w2) throws RepositoryException {
		if (w1 == null || w2 == null) {
			return;
		}
		JdbcTemplate template = DataSourceManager.getJdbcTemplate();
        String s = template.find("select s from t_goods_words_similar where w=?",
                new Object[] { w1 }, StringPropertyHandler.getInstance());
		List<String> words = null;
		if (s == null) {
			words = new ArrayList<String>(0);
		} else {
			words = StringUtil.split(s, ",");
		}
		if (!words.contains(w1)) {
			words.add(w1);
		}
		if (!words.contains(w2)) {
			words.add(w2);
		}
		for (String word : words) {
			List<String> similarWords = new ArrayList<String>(words);
			similarWords.remove(word);
			String similarWordsAsStr = StringUtil.join(similarWords, ",");
			boolean updated = template.update(
                    "update t_goods_words_similar set s=? where w=?", new Object[] {
                            similarWordsAsStr, word }) > 0;
            if (!updated) {
                template.update("insert into t_goods_words_similar(w,s) values(?,?)",
                        new Object[] { word, similarWordsAsStr });
			}
		}

	}
}
