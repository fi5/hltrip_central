package com.huoli.trip.central.web.controller;

import com.huoli.trip.central.api.IBaseDataService;
import com.huoli.trip.central.api.OrderService;
import com.huoli.trip.central.api.ProductService;
import com.huoli.trip.central.web.service.impl.YcfOrderManger;
import com.huoli.trip.common.vo.PriceInfo;
import com.huoli.trip.common.vo.request.BookCheckReq;
import com.huoli.trip.common.vo.request.CreateOrderReq;
import com.huoli.trip.common.vo.request.OrderOperReq;
import com.huoli.trip.common.vo.request.PayOrderReq;
import com.huoli.trip.common.vo.request.central.ProductPriceReq;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.order.OrderDetailRep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    YcfOrderManger ycfOrderManger;
    @Autowired
    private OrderService orderService;

    @Autowired
    ProductService productService;
    @Autowired
    IBaseDataService baseDataService;


//    @RequestMapping(value = "getCheckInfos", produces = {"application/json;charset=UTF-8"})
//    public Object getCheckInfos(@RequestBody BookCheckReq request) {
//        OrderService orderService =
//                (OrderService) SpringBeanFactoryUtil.getBean(ValidateUtils.checkChannalCode(request) +
//                        "OrderServiceImpl");
//        return orderService.getCheckInfos(request);
//    }
    @RequestMapping(value = "getCheckInfos",method = {RequestMethod.POST, RequestMethod.GET})
    public Object getCheckInfos(@RequestBody BookCheckReq request) {
        Object checkInfos = null;
        try {
            checkInfos = orderService.getCheckInfos(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return checkInfos;
    }

    @RequestMapping(value = "test")
    public Object test(String channel) {
//        testService.test(channel);
//        return channel;
      return  baseDataService.queryCitys();
    }
    @RequestMapping(value = "testZ")
    public Object testZ(String orderId) {
        OrderOperReq req=new OrderOperReq();
        req.setChannelCode("ycf");
        req.setOrderId(orderId);
        final BaseResponse<OrderDetailRep> orderDetail = ycfOrderManger.getOrderDetail(req);
        return orderDetail;
    }

    @RequestMapping(value = "testPrice")
    public Object testPrice(String productCode) {
        ProductPriceReq productPriceReq=new ProductPriceReq();
        productPriceReq.setProductCode(productCode);
        final BaseResponse<List<PriceInfo>> listBaseResponse = productService.productPriceCalendar(productPriceReq);
        return listBaseResponse;
    }

    @RequestMapping(value = "createOrder",method = {RequestMethod.POST, RequestMethod.GET})
    public Object createOrder(@RequestBody CreateOrderReq request) {
        Object createOrderRes = null;
        try {
            createOrderRes = orderService.createOrder(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return createOrderRes;
    }

    @RequestMapping(value = "payOrder",method = {RequestMethod.POST, RequestMethod.GET})
    public Object payOrder(@RequestBody PayOrderReq request) {
        Object payOrderRes = null;
        try {
            payOrderRes = orderService.payOrder(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return payOrderRes;
    }


}
