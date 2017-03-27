package com.movision.fsearch.core;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.movision.fsearch.analysis.TokenUtil;
import com.movision.fsearch.exception.ArgumentApiException;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;

import com.chenlb.mmseg4j.MMSeg;
import com.chenlb.mmseg4j.Word;
import com.petkit.base.utils.CollectionUtil;
import com.petkit.base.utils.FormatUtil;

public class QueryUtil {

	private static final SearchField DEFAULT_SEARCH_FIELD = new SearchField();
	static {
		DEFAULT_SEARCH_FIELD.setType(SearchFieldType.TYPE_STRING);
	}

    /**
     * 解析Query对象
     *
     * @param searcher
     * @param queryAsMap
     * @return
     * @throws Exception
     */
    public static Query parseQuery(Searcher searcher,
			Map<String, Object> queryAsMap) throws Exception {
		if (queryAsMap == null || queryAsMap.isEmpty()) {
			return new MatchAllDocsQuery();
		}
		List<Query> queries = new ArrayList<>(queryAsMap.size());
		for (Entry<String, Object> entry : queryAsMap.entrySet()) {
			String field = entry.getKey();
			Object value = entry.getValue();
			if (value == null) {
				throw new ArgumentApiException("query");
			}
			Map<?, ?> valueAsMap = (Map<?, ?>) value;
			Query query;
			String type = FormatUtil.parseString(valueAsMap.get("type"));
			value = valueAsMap.get("value");
			SearchField sField = searcher.getOptions().findField(field);
			if (sField == null) {
				sField = DEFAULT_SEARCH_FIELD;
            }
            switch (type) {
                /**
                 * 精确查询
				 */
				case "equal":
					if (sField.getType() == SearchFieldType.TYPE_STRING) {
						query = new TermQuery(new Term(field, value.toString()));
					} else if (sField.getType() == SearchFieldType.TYPE_INT) {
						int intValue = FormatUtil.parseInteger(value);
						query = NumericRangeQuery.newIntRange(field, intValue,
								intValue, true, true);
					} else if (sField.getType() == SearchFieldType.TYPE_LONG) {
						long longValue = FormatUtil.parseLong(value);
						query = NumericRangeQuery.newLongRange(field, longValue,
								longValue, true, true);
					} else if (sField.getType() == SearchFieldType.TYPE_FLOAT) {
						float floatValue = FormatUtil.parseFloat(value);
						query = NumericRangeQuery.newFloatRange(field, floatValue,
								floatValue, true, true);
					} else if (sField.getType() == SearchFieldType.TYPE_DOUBLE) {
						double doubleValue = FormatUtil.parseFloat(value);
						query = NumericRangeQuery.newDoubleRange(field,
								doubleValue, doubleValue, true, true);
					} else {
						throw new ArgumentApiException("query");
                    }
                    break;
                /**
                 * 区间查询
				 */
				case "numberrange":
					boolean minInclusive = FormatUtil.parseBoolean(valueAsMap
							.get("minInclusive"));
					boolean maxInclusive = FormatUtil.parseBoolean(valueAsMap
							.get("maxInclusive"));
					if (sField.getType() == SearchFieldType.TYPE_INT) {
						query = NumericRangeQuery.newIntRange(field,
								FormatUtil.parseInteger(valueAsMap.get("min")),
								FormatUtil.parseInteger(valueAsMap.get("max")),
								minInclusive, maxInclusive);
					} else if (sField.getType() == SearchFieldType.TYPE_LONG) {
						query = NumericRangeQuery.newLongRange(field,
								FormatUtil.parseLong(valueAsMap.get("min")),
								FormatUtil.parseLong(valueAsMap.get("max")),
								minInclusive, maxInclusive);
					} else if (sField.getType() == SearchFieldType.TYPE_FLOAT) {
						query = NumericRangeQuery.newFloatRange(field,
								FormatUtil.parseFloat(valueAsMap.get("min")),
								FormatUtil.parseFloat(valueAsMap.get("max")),
								minInclusive, maxInclusive);
					} else if (sField.getType() == SearchFieldType.TYPE_DOUBLE) {
						query = NumericRangeQuery.newDoubleRange(field,
								FormatUtil.parseDouble(valueAsMap.get("min")),
								FormatUtil.parseDouble(valueAsMap.get("max")),
								minInclusive, maxInclusive);
					} else {
						throw new ArgumentApiException("query");
                    }
                    break;
                /**
				 *
				 */
				case "phrase":
					MMSeg mmSeg = new MMSeg(new StringReader(value.toString()),
							TokenUtil.getComplexSeg());
					Word word;
					BooleanQuery phraseQuery = new BooleanQuery();
					// PhraseQuery phraseQuery = new PhraseQuery();
					// phraseQuery.setSlop(Integer.MAX_VALUE);
					while ((word = mmSeg.next()) != null) {
						String w = word.getString();
                        List<String> similarWords = SimilarWordManager.findSimilarWords(w);
                        Query q;
						if (CollectionUtil.isEmpty(similarWords)) {
							q = new TermQuery(new Term(field, w));
						} else {
							BooleanQuery bq = new BooleanQuery();
							bq.setMinimumNumberShouldMatch(1);
							for (String similarWord : similarWords) {
								bq.add(new TermQuery(new Term(field, similarWord)),
										BooleanClause.Occur.SHOULD);
							}
							q = bq;
						}
						phraseQuery.add(q, BooleanClause.Occur.MUST);
					}
					if (phraseQuery.getClauses().length == 1) {
						query = phraseQuery.getClauses()[0].getQuery();
					} else {
						query = phraseQuery;
					}
					// QueryBuilder qBuilder = new QueryBuilder(Searcher.ANALYZER);
					// query = qBuilder.createPhraseQuery(field, value.toString());
					break;
				default:
					throw new ArgumentApiException("query");
			}
			queries.add(query);
		}
		if (queries.size() == 1) {
			return queries.get(0);
		} else {
			BooleanQuery booleanQuery = new BooleanQuery();
			for (Query query : queries) {
				booleanQuery.add(query, BooleanClause.Occur.MUST);
			}
			return booleanQuery;
		}
	}

	public static SortField[] parseSort(List<?> sorts) {
		SortField[] sortFields = null;
		if (CollectionUtil.isNotEmpty(sorts)) {
			sortFields = new SortField[sorts.size()];
			Set<String> fields = new HashSet<>(sorts.size());
			int i = 0;
			for (Object sort : sorts) {
				Map<?, ?> sortAsMap = (Map<?, ?>) sort;
				String field = FormatUtil.parseString(sortAsMap.get("field"));
				if (fields.contains(field)) {
					continue;
				}
				fields.add(field);
				SortField.Type type = SortField.Type.valueOf(FormatUtil
                        .parseString(sortAsMap.get("type")).toUpperCase());
                boolean reverse = FormatUtil.parseBoolean(sortAsMap
						.get("reverse"));
				sortFields[i] = new SortField(field, type, reverse);
				i++;
			}
		}
		return sortFields;
	}
}
