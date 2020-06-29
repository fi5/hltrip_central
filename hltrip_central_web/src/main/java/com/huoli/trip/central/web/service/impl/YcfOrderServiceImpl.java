package com.huoli.trip.central.web.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.huoli.trip.central.api.OrderService;
import com.huoli.trip.central.web.service.OrderConsumerService;
import com.huoli.trip.common.vo.request.BookCheckReq;
import com.huoli.trip.common.vo.request.OrderStatusRequest;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfBookCheckReq;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 描述: <br> yaochufa serviceImpl
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：王德铭<br>
 * 版本：1.0<br>
 * 创建日期：2020/6/24<br>
 */
@Service
public class YcfOrderServiceImpl implements OrderService {
    @Autowired
    OrderConsumerService orderConsumerService;
    @Override
    public Object getOrderStatus(OrderStatusRequest request) {
        String orderId = request.getOrderId();
        if(StringUtils.isEmpty(orderId)){
            return "请求参数订单号为空";
        }
        return orderConsumerService.getYaochufaOrderStatus(orderId);
    }

    @Override
    public Object getCheckInfos(BookCheckReq checkReq) {
        YcfBookCheckReq req = new YcfBookCheckReq();
        BeanUtils.copyProperties(checkReq,req);
        return orderConsumerService.getYcfCheckInfos(req);
    }
}
