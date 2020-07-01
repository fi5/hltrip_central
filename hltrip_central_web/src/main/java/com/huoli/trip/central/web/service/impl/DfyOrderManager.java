package com.huoli.trip.central.web.service.impl;

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
public class DfyOrderManager extends OrderManager {

    public final static String CHANNEL="dfy";
    public String getChannel(){
        return CHANNEL;
    }
    @Override
    public String test() {
        System.out.println("dfy");
        return "dfy";
    }
}
