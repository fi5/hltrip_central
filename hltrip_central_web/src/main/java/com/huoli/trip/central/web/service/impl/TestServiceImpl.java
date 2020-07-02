package com.huoli.trip.central.web.service.impl;

import com.huoli.trip.central.web.dao.CityDao;
import com.huoli.trip.central.web.service.OrderFactory;
import com.huoli.trip.central.web.service.TestService;
import com.huoli.trip.common.entity.CityPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
    @Autowired
    CityDao  cityDao;

    @Override
    public void test(String channel) {
        OrderManager orderManager =orderFactory.getOrderManager(channel);
        if(orderManager==null){
            return;
        }
        final List<CityPO> cityPOs = cityDao.queryCitys();

        orderManager.test();
    }
}
