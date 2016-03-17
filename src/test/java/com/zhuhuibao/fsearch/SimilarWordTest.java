package com.zhuhuibao.fsearch;

import java.util.List;

import junit.framework.TestCase;

import com.petkit.base.utils.CollectionUtil;
import com.petkit.base.utils.StringUtil;
import com.zhuhuibao.fsearch.core.SimilarWordManager;

public class SimilarWordTest extends TestCase {

	public void testFind() throws Exception {
		List<String> words = SimilarWordManager.findSimilarWords("摄像头");
		System.out.println(StringUtil.join(words, ","));
		assertTrue(CollectionUtil.isNotEmpty(words));
	}

	public void testCreate() throws Exception {
		SimilarWordManager.create("摄像头", "摄像机");
	}

}
