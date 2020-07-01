package com.huoli.trip.central.web.service.impl;

import com.huoli.trip.central.web.service.OrderFactory;
import com.huoli.trip.central.web.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 描述：desc<br>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：顾刘川<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/1<br>
 */


@Service
public class TestServiceImpl implements TestService {

    @Autowired
    OrderFactory orderFactory;

    @Override
    public void test(String channel) {
        OrderManager orderManager =orderFactory.getOrderManager(channel);
        if(orderManager==null){
            return;
        }
        orderManager.test();
    }
}
