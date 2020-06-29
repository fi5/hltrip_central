package com.huoli.trip.central.web.controller;

import com.huoli.trip.central.api.OrderService;
import com.huoli.trip.central.web.util.SpringBeanFactoryUtil;
import com.huoli.trip.common.vo.request.BookCheckReq;
import org.springframework.web.bind.annotation.*;

/**
 * 描述: <br> 可预订查询
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：王德铭<br>
 * 版本：1.0<br>
 * 创建日期：2020/6/24<br>
 */
@RestController
@RequestMapping("hltrip")
public class Test {
    @RequestMapping(value = "getCheckInfos",produces={"application/json;charset=UTF-8"})
    public Object getCheckInfos(@RequestBody BookCheckReq request){
        OrderService orderService = (OrderService) SpringBeanFactoryUtil.getBean(request.getChannelCode() + "OrderServiceImpl");
        return orderService.getCheckInfos(request);
    }
}
