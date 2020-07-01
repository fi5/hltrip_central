package com.huoli.trip.central.web.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.huoli.trip.common.vo.request.BookCheckReq;
import com.huoli.trip.supplier.api.YcfOrderService;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfBookCheckReq;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfBookCheckRes;
import com.huoli.trip.supplier.self.yaochufa.vo.basevo.YcfBaseResult;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * 描述：desc<br>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：顾刘川<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/1<br>
 */
@Component
public class YfcOrderManger extends OrderManager {
    @Reference(group = "hllx")
    private YcfOrderService ycfOrderService;
    public final static String CHANNEL="ycf";
    public String getChannel(){
        return CHANNEL;
    }
    public String test() {
            System.out.println("ycf");
            return "ycf";
    }
    public Object getNBCheckInfos(BookCheckReq req) throws Exception {
        YcfBaseResult<YcfBookCheckRes> checkInfos = new YcfBaseResult<>();
        YcfBookCheckReq reqest = new YcfBookCheckReq();
        BeanUtils.copyProperties(req,reqest);
        try {
            checkInfos = ycfOrderService.getCheckInfos(reqest);
        }catch (Exception e){
            throw new RuntimeException("ycfOrderService --> rpc服务异常。。");
        }
        return checkInfos.getData();
    }
}
