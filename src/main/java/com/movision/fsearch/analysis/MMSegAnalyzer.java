package com.movision.fsearch.analysis;

import org.apache.lucene.analysis.Analyzer;

import com.chenlb.mmseg4j.Dictionary;
import com.chenlb.mmseg4j.MaxWordSeg;
import com.chenlb.mmseg4j.Seg;

/**
 * 默认使用 max-word
 *
 * mmseg4j 扩展 lucene的analyzer，叫MMSegAnalyzer默认使用Complex方式，也可以用SimpleAnalyzer（Simple方式）。
 *
 * @see {@link SimpleAnalyzer}, {@link ComplexAnalyzer}, {@link MaxWordAnalyzer}
 *
 * @author chenlb
 */
public class MMSegAnalyzer extends Analyzer {

	protected Dictionary dic;

	/**
	 * @see Dictionary#getInstance()
	 */
	public MMSegAnalyzer() {
		dic = TokenUtil.getDictionary();
	}

	protected Seg newSeg() {
		return new MaxWordSeg(dic);
	}

	public Dictionary getDict() {
		return dic;
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		return new TokenStreamComponents(new MMSegTokenizer(newSeg()));
	}
}
