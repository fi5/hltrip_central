package com.huoli.trip.central.web.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.huoli.trip.supplier.api.YcfOrderService;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfBookCheckReq;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfCreateOrderReq;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfPayOrderReq;
import org.springframework.stereotype.Component;

/**
 * 接收供应商层的dubbo服务并做业务处理
 */
@Component
public class OrderConsumerService {
    @Reference( group = "hllx")
    YcfOrderService ycfOrderService;

//*************************************【要出发】*******************************************

    public Object getYaochufaOrderStatus(String orderId){
        ycfOrderService.getOrder(orderId);
        return null;

    }

    public Object getYcfCheckInfos(YcfBookCheckReq checkRequest){
        return ycfOrderService.getCheckInfos(checkRequest);
    }

    public Object createOrder(YcfCreateOrderReq createOrderReq){
        return ycfOrderService.createOrder(createOrderReq);
    }

    public Object payOrder(YcfPayOrderReq payOrderReq){
        return ycfOrderService.payOrder(payOrderReq);
    }

//*************************************【笛风云】*******************************************
    public Object getDiFengYunOrderStatus(String orderId){
        return null;
    }

}
