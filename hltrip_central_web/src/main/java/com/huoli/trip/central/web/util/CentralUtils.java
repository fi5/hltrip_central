package com.huoli.trip.central.web.util;

import org.apache.commons.lang3.StringUtils;

/**
 * 描述: <br>中台业务工具类
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：王德铭<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/5<br>
 */
public class CentralUtils {

    //通过前端传的productCode获取供应商渠道标识
    public static String getChannelCode(String productCode){
        if(StringUtils.isBlank(productCode)){
            return null;
        }
        String[] s = productCode.split("_");
        return s[0];
    }
    //将前端传的productCode获取转为要出发的productId
    public static String getSupplierId(String productCode){
        if(StringUtils.isBlank(productCode)){
            return null;
        }
        return productCode.substring(productCode.indexOf("_")+1);
    }

    //api会传到中台
//    //生成支付流水号
//    public static String makeSerialNumber(String orderNo) {
//        return String.valueOf(new Date().getTime())+Math.abs(orderNo.hashCode());
//    }


    // 根据Unicode编码判断中文汉字和符号
    public static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION) {
            return true;
        }
        return false;
    }
}
