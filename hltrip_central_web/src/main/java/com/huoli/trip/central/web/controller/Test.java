package com.huoli.trip.central.web.controller;

import com.alibaba.fastjson.JSONObject;
import com.huoli.trip.central.api.BaseDataService;
import com.huoli.trip.central.api.OrderService;
import com.huoli.trip.central.api.ProductService;
import com.huoli.trip.central.web.service.impl.YcfOrderManger;
import com.huoli.trip.common.vo.request.*;
import com.huoli.trip.common.vo.request.central.CityReq;
import com.huoli.trip.common.vo.request.central.ProductPriceReq;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.central.ProductPriceCalendarResult;
import com.huoli.trip.common.vo.response.central.ProductPriceDetialResult;
import com.huoli.trip.common.vo.response.order.OrderDetailRep;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class Test {

    @Autowired
    YcfOrderManger ycfOrderManger;
    @Autowired
    private OrderService orderService;

    @Autowired
    ProductService productService;
    @Autowired
    BaseDataService baseDataService;


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
    public Object test(CityReq  req) {
//        testService.test(channel);
//        return charennel;
        return  baseDataService.queryCitys(req);
    }
    @RequestMapping(value = "testZ")
    public Object testZ(String orderId) {
        OrderOperReq req=new OrderOperReq();
        req.setChannelCode("ycf");
        req.setOrderId(orderId);
        final BaseResponse<OrderDetailRep> orderDetail = ycfOrderManger.getOrderDetail(req);
        return orderDetail;
    }

    @RequestMapping(value = "testkafka")
    public String testkafka() {
        RefundNoticeReq req=new RefundNoticeReq();
        req.setPartnerOrderId("1234");
         orderService.refundNotice(req);
        return "ok";
    }

    @RequestMapping(value = "testPrice")
    public Object testPrice(ProductPriceReq productPriceReq) {
        final BaseResponse<ProductPriceCalendarResult> listBaseResponse = productService.productPriceCalendar(productPriceReq);
        return listBaseResponse;
    }

    @RequestMapping(value = "priceDetail")
    public BaseResponse<ProductPriceDetialResult> priceDetail(ProductPriceReq req) {
        final BaseResponse<ProductPriceDetialResult> priceDetail = productService.getPriceDetail(req);
        return priceDetail;
    }


    @RequestMapping(value = "getVoucher")
    public BaseResponse<OrderDetailRep> getVochers(OrderOperReq req)  {
        final BaseResponse<OrderDetailRep> vochers = orderService.getVochers(req);
        return vochers;
    }

    @RequestMapping(value = "getOrder")
    public BaseResponse<OrderDetailRep> getOrder(OrderOperReq req)  {
        final BaseResponse<OrderDetailRep> order = orderService.getOrder(req);
        return order;
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

    @RequestMapping(value = "cancelOrder",method = {RequestMethod.POST, RequestMethod.GET})
    public Object cancelOrder(@RequestBody CancelOrderReq request) {
        Object cancelOrderRes = null;
        try {
            cancelOrderRes = orderService.cancelOrder(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cancelOrderRes;
    }

    @RequestMapping(value = "payCheck",method = {RequestMethod.POST, RequestMethod.GET})
    public Object payCheck(@RequestBody PayOrderReq request) {
        Object payCheck = null;
        try {
            payCheck = orderService.payCheck(request);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return payCheck;
    }

    @RequestMapping(value = "testOrderStatuskafka")
    public String testOrderStatuskafka() {
        PushOrderStatusReq req = new PushOrderStatusReq();
        req.setPartnerOrderId("1234");
        req.setRemark("订单状态改变了");
        req.setOrderStatus(0);
        try {
            orderService.orderStatusNotice(req);
        }catch (Exception e){
            log.error("testOrderStatuskafka 异常了"+ JSONObject.toJSONString(req),e);
        }
        return "ok";
    }
}
