package com.huoli.trip.central.web.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.huoli.trip.central.api.OrderService;
import com.huoli.trip.central.web.service.OrderConsumerService;
import com.huoli.trip.common.vo.request.BookCheckReq;
import com.huoli.trip.common.vo.request.OrderStatusRequest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 描述: <br> dfy serviceImpl
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：王德铭<br>
 * 版本：1.0<br>
 * 创建日期：2020/6/24<br>
 */
@Service
public class DfyOrderServiceImpl implements OrderService {
    @Autowired
    OrderConsumerService orderConsumerService;
    @Override
    public Object getOrderStatus(OrderStatusRequest request) {
        return null;
    }

    @Override
    public Object getCheckInfos(BookCheckReq req) {
        return null;
    }
}
