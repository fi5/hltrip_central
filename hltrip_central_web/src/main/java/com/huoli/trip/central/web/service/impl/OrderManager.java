package com.huoli.trip.central.web.service.impl;

/**
 * 描述：desc<br>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：顾刘川<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/1<br>
 */
public abstract class OrderManager {
    public final static String CHANNEL="order";

    public String getChannel(){
        return CHANNEL;
    }

    public  String test(){
        return "cha";
    }

}
