package com.movision.fsearch.analysis;

import com.chenlb.mmseg4j.Seg;

/**
 * mmseg 的 complex analyzer
 *
 * ex:羽毛球拍,研究生命起源,国际化,眼看就要来了,为首要考虑
 * 羽毛 | 球拍 | 研究 | 生命 | 起源 | 国际化 | 眼看 | 就要 | 来 | 了 | 为首 | 要 | 考虑 |
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
