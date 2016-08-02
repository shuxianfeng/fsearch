package com.zhuhuibao.fsearch.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 公式
 *
 * @author jianglz
 * @since 2016/8/2.
 */
public class FormulaUtil {

    // 默认除法算法精确度
    private static final int DEF_DIV_SCALE = 10;

    /**
     * 提供精确的加法运算
     *
     * @param v1
     * @param v2
     * @return
     */
    public static double add(double v1, double v2) {
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));

        return b1.add(b2).doubleValue();
    }

    /**
     * 提供精确的减法运算
     *
     * @param v1
     * @param v2
     * @return
     */
    public static double sub(double v1, double v2) {
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));

        return b1.subtract(b2).doubleValue();
    }

    /**
     * 提供精确的乘法运算
     *
     * @param v1
     * @param v2
     * @return
     */
    public static double mul(double v1, double v2) {
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));

        return b1.multiply(b2).doubleValue();
    }

    /**
     * 提供（相对）精确的除法运算，当发生除不尽的情况时，精确到
     * 小数点后10位，以后的数字四舍五入
     *
     * @param v1
     * @param v2
     * @return
     */
    public static double div(double v1, double v2) {
        return div(v1, v2, DEF_DIV_SCALE);
    }

    /**
     * 提供（相对）精确的除法运算，当发生除不尽的情况时，由scale参数
     * 指定精度，以后的数字四舍五入
     *
     * @param v1
     * @param v2
     * @param scale
     * @return
     */
    public static double div(double v1, double v2, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("The scale must be a positive integer or zero");
        }
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));

        return b1.divide(b2, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * 提供精确的小数位四舍五入处理
     *
     * @param v     需要四舍五入的数字
     * @param scale 小数点后保留几位
     * @return 四舍五入后的结果
     */
    public static double round(double v, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("The scale must be a positive integer or zero");
        }
        BigDecimal b = new BigDecimal(Double.toString(v));
        BigDecimal one = new BigDecimal("1");

        return b.divide(one, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * 计算权重
     *
     * @return
     */
    public static double calWeight(List<Factor> factorList) {
        double result = 0;
        for (Factor factor : factorList) {
            double weight = factor.getWeight();
            double score = factor.getScore();
            double tmp = FormulaUtil.mul(weight,score);
            result =  FormulaUtil.add(result,tmp);
        }
        return result;
    }


    public static void main(String[] args) {
        List<Factor> list = new ArrayList<>();
        Factor factor1 = new Factor();
        factor1.setWeight(4);
        factor1.setScore(12);
        list.add(factor1);

        factor1 = new Factor();
        factor1.setWeight(6);
        factor1.setScore(5);
        list.add(factor1);

       double result =  FormulaUtil.calWeight(list);
        System.out.println(result);
    }
}
