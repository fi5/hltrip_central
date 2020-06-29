package com.huoli.trip.central.web.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.huoli.trip.supplier.api.YcfOrderService;
import com.huoli.trip.supplier.api.YcfOrderStatusService;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfBookCheckReq;
import org.springframework.stereotype.Component;

/**
 * 接收供应商层的dubbo服务并做业务处理
 */
@Component
public class OrderConsumerService {
    @Reference( group = "hllx")
    YcfOrderStatusService ycfOrderStatusService;
    @Reference( group = "hllx")
    YcfOrderService ycfOrderService;

    public Object getYaochufaOrderStatus(String orderId){
        ycfOrderStatusService.getOrder(orderId);
        return null;

    }

    public Object getYcfCheckInfos(YcfBookCheckReq checkRequest){
        return ycfOrderService.getCheckInfos(checkRequest);
    }

    public Object getDiFengYunOrderStatus(String orderId){
        return null;
    }

}
