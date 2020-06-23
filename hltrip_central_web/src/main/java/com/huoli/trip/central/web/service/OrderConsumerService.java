package com.huoli.trip.central.web.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.huoli.trip.supplier.api.YcfOrderStatusService;

/**
 * 接收供应商层的dubbo服务并做业务处理
 */
public class OrderConsumerService {
    @Reference( group = "hllx")
    YcfOrderStatusService ycfOrderStatusService;

    public Object getYaochufaOrderStatus(String orderId){
        ycfOrderStatusService.getOrder(orderId);
        return null;

    }

    public void getDiFengYunOrderStatus(String orderId){
        return;
    }

}
