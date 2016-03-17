package com.zhuhuibao.fsearch.analysis;

import com.chenlb.mmseg4j.Seg;

/**
 * mmseg 的 complex analyzer
 * 
 * @author chenlb 2009-3-16 下午10:08:16
 */
public class ComplexAnalyzer extends MMSegAnalyzer {

	public ComplexAnalyzer() {
		super();
	}

	protected Seg newSeg() {
		return TokenUtil.getComplexSeg();
	}
}
