package com.huoli.trip.central.web.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;

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
    public static String getSupplierId(String productCode){
        if(StringUtils.isBlank(productCode)){
            return null;
        }
        String[] s = productCode.split("_");
        return s[0];
    }

    //生成支付流水号
    public static String makeSerialNumber(String orderNo) {
        return String.valueOf(new Date().getTime())+Math.abs(orderNo.hashCode());
    }
}
