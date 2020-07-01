package com.huoli.trip.central.web.controller;

import com.huoli.trip.central.api.OrderService;
import com.huoli.trip.central.web.service.TestService;
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
    private OrderService orderService;
    @Autowired
    private TestService testService;
//    @RequestMapping(value = "getCheckInfos", produces = {"application/json;charset=UTF-8"})
//    public Object getCheckInfos(@RequestBody BookCheckReq request) {
//        OrderService orderService =
//                (OrderService) SpringBeanFactoryUtil.getBean(ValidateUtils.checkChannalCode(request) +
//                        "OrderServiceImpl");
//        return orderService.getCheckInfos(request);
//    }
    @RequestMapping(value = "getCheckInfos",method = {RequestMethod.POST, RequestMethod.GET})
    public BaseResponse getCheckInfos(@RequestBody BookCheckReq request) {
        Object checkInfos = null;
        try {
            checkInfos = orderService.getCheckInfos(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new BaseResponse().withSuccess(checkInfos);
    }

    @RequestMapping(value = "test")
    public Object test(String channel) {
        testService.test(channel);
        return channel;
    }
}
