package com.huoli.trip.central.web.controller;

import com.huoli.trip.central.api.OrderService;
import com.huoli.trip.central.web.service.TestService;
import com.huoli.trip.central.web.service.impl.YcfOrderManger;
import com.huoli.trip.central.web.util.SpringBeanFactoryUtil;
import com.huoli.trip.central.web.util.ValidateUtils;
import com.huoli.trip.common.vo.request.BookCheckReq;
import com.huoli.trip.common.vo.request.OrderOperReq;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.order.OrderDetailRep;
import com.huoli.trip.common.vo.request.BookCheckReq;
import com.huoli.trip.common.vo.response.BaseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 描述: <br> 可预订查询
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：王德铭<br>
 * 版本：1.0<br>
 * 创建日期：2020/6/24<br>
 */
@RestController
public class Test {
    @Autowired
    TestService testService;
    @Autowired
    YcfOrderManger ycfOrderManger;

//    @RequestMapping(value = "getCheckInfos", produces = {"application/json;charset=UTF-8"})
//    public Object getCheckInfos(@RequestBody BookCheckReq request) {
//        OrderService orderService =
//                (OrderService) SpringBeanFactoryUtil.getBean(ValidateUtils.checkChannalCode(request) +
//                        "OrderServiceImpl");
//        try {
//            return orderService.getCheckInfos(request);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
    private OrderService orderService;
//    @RequestMapping(value = "getCheckInfos", produces = {"application/json;charset=UTF-8"})
//    public Object getCheckInfos(@RequestBody BookCheckReq request) {
//        OrderService orderService =
//                (OrderService) SpringBeanFactoryUtil.getBean(ValidateUtils.checkChannalCode(request) +
//                        "OrderServiceImpl");
//        return orderService.getCheckInfos(request);
//    }

    @RequestMapping(value = "test")
    public Object test(String channel) {
        testService.test(channel);
        return channel;
    }
    @RequestMapping(value = "testZ")
    public Object testZ(String orderId) {
        OrderOperReq req=new OrderOperReq();
        req.setChannelCode("ycf");
        req.setOrderId(orderId);
        final BaseResponse<OrderDetailRep> orderDetail = ycfOrderManger.getOrderDetail(req);
        return orderDetail;
    }
}
