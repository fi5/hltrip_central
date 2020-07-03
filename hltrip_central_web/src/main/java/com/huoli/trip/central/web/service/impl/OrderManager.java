package com.huoli.trip.central.web.service.impl;

import com.huoli.trip.common.vo.request.CreateOrderReq;
import com.huoli.trip.common.vo.request.OrderOperReq;
import com.huoli.trip.common.vo.request.PayOrderReq;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.order.CenterBookCheckRes;
import com.huoli.trip.common.vo.response.order.CenterCreateOrderRes;
import com.huoli.trip.common.vo.response.order.OrderDetailRep;
import com.huoli.trip.common.vo.request.BookCheckReq;
import com.huoli.trip.common.vo.response.order.CenterPayOrderRes;

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

    public BaseResponse<OrderDetailRep> getOrderDetail(OrderOperReq req){

        return null;
    }
    public BaseResponse<OrderDetailRep> getVochers(OrderOperReq req) {
        return null;
    }
    public CenterBookCheckRes.CenterBookCheck getNBCheckInfos(BookCheckReq req) throws Exception {
        return null;
    }
    public CenterCreateOrderRes getNBCreateOrder(CreateOrderReq req) throws Exception {
        return null;
    }

    public CenterPayOrderRes getCenterPayOrder(PayOrderReq req) throws Exception{
        return null;
    }
}
