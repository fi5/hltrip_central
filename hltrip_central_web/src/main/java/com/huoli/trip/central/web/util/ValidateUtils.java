package com.huoli.trip.central.web.util;

import com.huoli.trip.common.constant.ChannelConstant;
import com.huoli.trip.common.vo.request.BaseRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;


/**
 * 描述: <br> 业务校验
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：王德铭<br>
 * 版本：1.0<br>
 * 创建日期：2020/6/24<br>
 */
@Slf4j
public class ValidateUtils {
    /**
     * 校验供应商渠道
     * @param request
     * @return
     */
    public static String checkChannalCode(BaseRequest request){
        String channelCode = request.getChannelCode();
        if(StringUtils.isEmpty(channelCode)){
            log.info("渠道信息不可为空 返回相关提示");
            return null;
        }else if(!StringUtils.equals(ChannelConstant.SUPPLIER_TYPE_YCF,channelCode)
                ||!StringUtils.equals(ChannelConstant.SUPPLIER_TYPE_YCF,channelCode)){
            log.info("渠道信息不存在,请检查相关配置");
            return null;
        }
        return channelCode;
    }
}
