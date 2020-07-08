package com.huoli.trip.central.web.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSONObject;
import com.huoli.trip.central.api.OrderService;
import com.huoli.trip.central.web.service.OrderFactory;
import com.huoli.trip.central.web.util.CentralUtils;
import com.huoli.trip.common.vo.request.*;
import com.huoli.trip.common.vo.request.central.ProductPriceReq;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.central.ProductPriceDetialResult;
import com.huoli.trip.common.vo.response.order.*;
import com.huoli.trip.supplier.api.YcfOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * 描述：订单方法处理<br>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：顾刘川<br>
 * 版本：1.0<br>
 * 创建日期：2020/6/30<br>
 */
@Slf4j
@Service(timeout = 10000,group = "hllx")
public class OrderServiceImpl implements OrderService {

    @Reference(group = "hllx")
    private YcfOrderService ycfOrderService;
    @Autowired
    KafkaTemplate kafkaTemplate;
    @Autowired
    OrderFactory orderFactory;

    @Override
    public Object getOrderStatus(OrderStatusRequest request) {
        return null;
    }

    @Override
    public BaseResponse<CenterBookCheckRes> getCheckInfos(BookCheckReq req) {
        OrderManager orderManager =orderFactory.getOrderManager(CentralUtils.getChannelCode(req.getProductId()));
        //校验manager处理
        checkManger(orderManager);
        //封装中台返回
        CenterBookCheckRes checkRes = new CenterBookCheckRes();
        //供应商对象包装业务实体类
        CenterSupplier supplier = new CenterSupplier();
        try {
            supplier.setData(orderManager.getNBCheckInfos(req));
            supplier.setType(req.getChannelCode());
            checkRes.setSupplier(supplier);
        }catch (RuntimeException e){
            log.error("orderManager --> rpc服务异常",e);
            BaseResponse.withFail(-100,e.getMessage());
            throw new RuntimeException("orderManager --> rpc服务异常",e);
        }
        return BaseResponse.withSuccess(checkRes);
    }

    @Override
    public void refundNotice(RefundNoticeReq req) {

        try {
            log.info("refundNotice发送kafka"+ JSONObject.toJSONString(req));
            String topic = "hltrip_order_refund";
            JSONObject kafkaInfo = new JSONObject();
            ListenableFuture<SendResult<String, String>> listenableFuture = kafkaTemplate.send(topic, JSONObject.toJSONString(kafkaInfo));
            listenableFuture.addCallback(
                    result -> log.info("订单发送kafka成功, params : {}", JSONObject.toJSONString(req)),
                    ex -> {
                        log.info("订单发送kafka失败, error message:{}", ex.getMessage(), ex);
                    });
        } catch (Exception e) {
        	log.info("",e);
        }

    }

    @Override
    public BaseResponse<OrderDetailRep> getOrder(OrderOperReq req) {
        OrderManager orderManager =orderFactory.getOrderManager(req.getChannelCode());
        if(orderManager==null){
            return null;
        }
        try {
            BaseResponse<OrderDetailRep> orderDetail = orderManager.getOrderDetail(req);
            return orderDetail;
        } catch (Exception e) {
        	log.info("",e);
            return  null;
        }

    }

    @Override
    public BaseResponse<OrderDetailRep> getVochers(OrderOperReq req) {
        OrderManager orderManager = orderFactory.getOrderManager(req.getChannelCode());
        if (orderManager == null) {
            return null;
        }
        BaseResponse<OrderDetailRep> orderDetail = orderManager.getVochers(req);
        return orderDetail;
    }

    @Override
    public BaseResponse<CenterCreateOrderRes> createOrder(CreateOrderReq req) {
        OrderManager orderManager = orderFactory.getOrderManager(CentralUtils.getChannelCode(req.getProductId()));
        //校验manager处理
        checkManger(orderManager);
        BaseResponse<CenterCreateOrderRes> result = new BaseResponse<>();
        CenterCreateOrderRes data = new CenterCreateOrderRes();
        try {
            data = orderManager.getNBCreateOrder(req);
        } catch (Exception e) {
            log.error("OrderServiceImpl --> createOrder rpc服务异常", e);
            result.fail(-100, result.getMessage(), false);
            throw new RuntimeException("OrderServiceImpl --> createOrder --> rpc服务异常");
        }
        return result.withSuccess(data);
    }

    @Override
    public BaseResponse<CenterPayOrderRes> payOrder(PayOrderReq req) {
        OrderManager orderManager = orderFactory.getOrderManager((req.getChannelCode()));
        //校验manager处理
        checkManger(orderManager);
        BaseResponse<CenterPayOrderRes> result = new BaseResponse<>();
        CenterPayOrderRes data = new CenterPayOrderRes();
        try {
            data = orderManager.getCenterPayOrder(req);
        } catch (Exception e) {
            log.error("OrderServiceImpl --> payOrder rpc服务异常", e);
            result.fail(-100, result.getMessage(), false);
            throw new RuntimeException("OrderServiceImpl --> payOrder --> rpc服务异常");
        }
        return result.withSuccess(data);
    }

    @Override
    public BaseResponse<CenterCancelOrderRes> cancelOrder(CancelOrderReq req) {
        OrderManager orderManager = orderFactory.getOrderManager(CentralUtils.getChannelCode(req.getProductCode()));
        //校验manager处理
        checkManger(orderManager);
        BaseResponse<CenterCancelOrderRes> result = new BaseResponse<>();
        CenterCancelOrderRes data = new CenterCancelOrderRes();
        try {
            data = orderManager.getCenterCancelOrder(req);
        } catch (Exception e) {
            log.error("OrderServiceImpl --> payOrder rpc服务异常", e);
            result.fail(-100, result.getMessage(), false);
            throw new RuntimeException("OrderServiceImpl --> payOrder --> rpc服务异常");
        }
        return result.withSuccess(data);
    }

    @Override
    public BaseResponse<CenterCancelOrderRes> applyRefund(CancelOrderReq req) {
        //todo 退款通知
        return cancelOrder(req);
    }

    @Override
    public void orderStatusNotice(PushOrderStatusReq req) {
        try {
            log.info("orderStatusNotice发送kafka"+ JSONObject.toJSONString(req));
            String topic = "hltrip_order_orderstatus";
            JSONObject kafkaInfo = new JSONObject();
            ListenableFuture<SendResult<String, String>> listenableFuture = kafkaTemplate.send(topic, JSONObject.toJSONString(kafkaInfo));
            listenableFuture.addCallback(
                    result -> log.info("订单状态推送kafka成功, params : {}", JSONObject.toJSONString(req)),
                    ex -> {
                        log.info("订单状态推送kafka失败, error message:{}", ex.getMessage(), ex);
                    });
        } catch (Exception e) {
            log.info("",e);
        }
    }

    /**
     * 校验manager
     * @param manager
     * @return
     */
    public Object checkManger(OrderManager manager){
        if(manager==null){
            log.info("供应商OrderManager爆了");
            return null;
        }
        return manager;
    }
}
