package com.huoli.trip.central.web.util;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.util.List;
import java.util.Optional;

/**
 * 描述: <br>中台业务工具类
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：王德铭<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/5<br>
 */
@Slf4j
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

    public static <T> void pinyinSort(List<T> list, Class<T> tClass, String fieldName) throws InstantiationException, IllegalAccessException {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        list.sort((T info1, T info2) -> {
            String province1 = null;
            String province2 = null;
            try {
                province1 = PinyinHelper.toHanYuPinyinString(getFieldValue(info1, fieldName, tClass), format, " ", true);
                province2 = PinyinHelper.toHanYuPinyinString(getFieldValue(info2, fieldName, tClass), format, " ", true);
            } catch (BadHanyuPinyinOutputFormatCombination badHanyuPinyinOutputFormatCombination) {
                log.error("汉字排序错误: {}", badHanyuPinyinOutputFormatCombination.getMessage());
            }
            return Optional.ofNullable(province1).orElse("").compareTo(province2);
        });
    }

    public static <T> String getFieldValue(Object target, String fieldName, Class<T> typeName) {
        try {
            Object fieldValue = FieldUtils.readField(target, fieldName, true);
            return (String) fieldValue;
        } catch (IllegalAccessException e) {
            log.error("出错:实体类{}没有{}类型的{}属性字段!", target.getClass(), typeName.getSimpleName(), fieldName);
            throw new RuntimeException(e);
        }
    }
}
