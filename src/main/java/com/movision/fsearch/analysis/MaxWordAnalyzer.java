package com.movision.fsearch.analysis;

import com.chenlb.mmseg4j.Seg;

/**
 * 最多分词方式.
 * 
 * @author chenlb 2009-4-6 下午08:43:46
 */
public class MaxWordAnalyzer extends MMSegAnalyzer {

	public MaxWordAnalyzer() {
		super();
	}

	protected Seg newSeg() {
		return TokenUtil.getMaxWordSeg();
	}
}
