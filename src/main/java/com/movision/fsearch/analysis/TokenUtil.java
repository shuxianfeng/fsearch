package com.movision.fsearch.analysis;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.chenlb.mmseg4j.ComplexSeg;
import com.chenlb.mmseg4j.Dictionary;
import com.chenlb.mmseg4j.MaxWordSeg;
import com.chenlb.mmseg4j.Seg;
import com.movision.fsearch.L;
import com.petkit.base.utils.FileUtil;
import com.petkit.base.utils.StringUtil;
import com.movision.fsearch.G;

public class TokenUtil {
    //词库文件
    private static File DICT_FILE = null;
	private static Dictionary DICT = null;
//	private static Seg COMPLEX_SEG = null;
//	private static Seg MAXWORD_SEG = null;

	static {
		DICT_FILE = new File(G.getConfig().getString("dict.path"));
		if (!DICT_FILE.exists()) {
			try {
				FileUtil.ensureFile(DICT_FILE);
			} catch (IOException e) {
				L.error("Failed to init dictionary", e);
				System.exit(-1);
			}
		}
		DICT = Dictionary.getInstance(DICT_FILE.getParentFile());
//		COMPLEX_SEG = ;
//		MAXWORD_SEG = ;
	}

	public static Dictionary getDictionary() {
		return DICT;
	}

	public static Seg getComplexSeg() {
		return new ComplexSeg(DICT);
	}

	public static Seg getMaxWordSeg() {
		return new MaxWordSeg(DICT);
	}

	public static File getDictionaryFile() {
		return DICT_FILE;
	}

	public static void reloadDictionary() {
		DICT.reload();
	}

    /**
     * 判断是否是中文字符
     *
     * @param c
     * @return
     */
    public static boolean isChineseChar(char c) {
		Character.UnicodeScript sc = Character.UnicodeScript.of(c);
		return sc == Character.UnicodeScript.HAN;
	}

	public static List<String> tokenizeChinese(String s) {
		if (StringUtil.isEmpty(s)) {
			return null;
		}
		List<String> list = new LinkedList<>();
		int i = 0;
		int len = s.length();
		while (i < len) {
			char c = s.charAt(i);
			StringBuilder buf = new StringBuilder();
			buf.append(c);
			i++;
			if (isChineseChar(c)) {
				for (; i < len; i++) {
					c = s.charAt(i);
					if (isChineseChar(c)) {
						buf.append(c);
					} else {
						break;
					}
				}
			} else {
				for (; i < len; i++) {
					c = s.charAt(i);
					if (!isChineseChar(c)) {
						buf.append(c);
					} else {
						break;
					}
				}
			}
			list.add(buf.toString());
        }
        return list;
    }

    /**
     * 校验中文字符，只添加中文字符
     *
     * @param items
     * @param set
     */
    public static void validateChineseCharAndAddToSet(List<String> items, Set<String> set) {
        for (String item : items) {
            List<String> tokens = TokenUtil.tokenizeChinese(item);
            if (tokens == null) {
                continue;
            }
            //中文字符校验，只添加中文字符
            for (String token : tokens) {
                if (TokenUtil.isChineseChar(token.charAt(0))) {
                    set.add(token);
                }
			}
		}
	}
}
