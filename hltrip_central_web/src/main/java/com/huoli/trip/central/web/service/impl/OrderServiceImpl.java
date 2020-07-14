package com.huoli.trip.central.web.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSONObject;
import com.huoli.trip.central.api.OrderService;
import com.huoli.trip.central.web.service.OrderFactory;
import com.huoli.trip.central.web.util.CentralUtils;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.constant.OrderStatus;
import com.huoli.trip.common.util.CommonUtils;
import com.huoli.trip.common.vo.request.*;
import com.huoli.trip.common.vo.request.central.OrderStatusKafka;
import com.huoli.trip.common.vo.request.central.RefundKafka;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.order.*;
import com.huoli.trip.supplier.api.YcfOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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
@Service(timeout = 10000,group = "hltrip")
public class OrderServiceImpl implements OrderService {

    @Reference(group = "hltrip")
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
    public BaseResponse<CenterBookCheck> getCheckInfos(BookCheckReq req) {
        OrderManager orderManager =orderFactory.getOrderManager(CentralUtils.getChannelCode(req.getProductId()));
        //校验manager处理
        checkManger(orderManager);
        //封装中台返回
        BaseResponse<CenterBookCheck> checkRes = orderManager.getCenterCheckInfos(req);;
        return checkRes;
    }

    @Override
    public void refundNotice(RefundNoticeReq req) {

        try {
            log.info("refundNotice发送kafka"+ JSONObject.toJSONString(req));
            String topic = "hltrip_order_refund";
            RefundKafka kafkaInfo = new RefundKafka();
            kafkaInfo.setOrderId(req.getPartnerOrderId());
            kafkaInfo.setRefundStatus(req.getRefundStatus());
            kafkaInfo.setRefundPrice(req.getRefundPrice());
            kafkaInfo.setExpense(req.getRefundCharge());
            kafkaInfo.setHandleRemark(req.getHandleRemark());
            kafkaInfo.setRefundReason(req.getRefundReason());
            if(null!=req.getRefundTime())
            kafkaInfo.setRefundTime(CommonUtils.dateFormat.format(req.getRefundTime()));
            if(null!=req.getResponseTime())
                kafkaInfo.setResponseTime(CommonUtils.dateFormat.format(req.getResponseTime()));
            ListenableFuture<SendResult<String, String>> listenableFuture = kafkaTemplate.send(topic, JSONObject.toJSONString(kafkaInfo));
            listenableFuture.addCallback(
                    result -> log.info("订单发送kafka成功, params : {}", JSONObject.toJSONString(req)),
                    ex -> {
                        log.info("订单发送kafka失败, error message:{}", ex.getMessage(), ex);
                    });
        } catch (Exception e) {
        	log.info("refundNotice写kafka时报错:"+JSONObject.toJSONString(req),e);
        }

    }

    @Override
    public BaseResponse<OrderDetailRep> getOrder(OrderOperReq req) {
        OrderManager orderManager =orderFactory.getOrderManager(req.getChannelCode());
        if(orderManager==null){
            return BaseResponse.fail(CentralError.NO_RESULT_ERROR);
        }
        try {
            BaseResponse<OrderDetailRep> orderDetail = orderManager.getOrderDetail(req);
            return orderDetail;
        } catch (Exception e) {
        	log.info("getOrder查询订单报错",e);
            return  BaseResponse.fail(CentralError.ERROR_SERVER_ERROR);
        }

    }

    @Override
    public BaseResponse<OrderDetailRep> getVochers(OrderOperReq req) {
        OrderManager orderManager = orderFactory.getOrderManager(req.getChannelCode());
        if (orderManager == null) {
            return BaseResponse.fail(CentralError.NO_RESULT_ERROR);
        }
        BaseResponse<OrderDetailRep> orderDetail = orderManager.getVochers(req);
        return orderDetail;
    }

    @Override
    public BaseResponse<CenterCreateOrderRes> createOrder(CreateOrderReq req) {
        OrderManager orderManager = orderFactory.getOrderManager(CentralUtils.getChannelCode(req.getProductId()));
        //校验manager处理
        checkManger(orderManager);
        BaseResponse<CenterCreateOrderRes> result = orderManager.getCenterCreateOrder(req);;
        return result;
    }

    @Override
    public BaseResponse<CenterPayOrderRes> payOrder(PayOrderReq req) {
        OrderManager orderManager = orderFactory.getOrderManager(req.getChannelCode());
        //校验manager处理
        checkManger(orderManager);
        BaseResponse<CenterPayOrderRes> result = orderManager.getCenterPayOrder(req);;
        return result;
    }

    @Override
    public BaseResponse<CenterCancelOrderRes> cancelOrder(CancelOrderReq req) {
        OrderManager orderManager = orderFactory.getOrderManager(CentralUtils.getChannelCode(req.getProductCode()));
        //校验manager处理
        checkManger(orderManager);
        BaseResponse<CenterCancelOrderRes> result = orderManager.getCenterCancelOrder(req);
        return result;
    }

    @Override
    public BaseResponse<CenterCancelOrderRes> applyRefund(CancelOrderReq req) {
        OrderManager orderManager = orderFactory.getOrderManager(req.getChannelCode());
        //校验manager处理
        checkManger(orderManager);
        BaseResponse<CenterCancelOrderRes> result = orderManager.getCenterApplyRefund(req);
        return result;
    }

    @Override
    public void orderStatusNotice(PushOrderStatusReq req) {
        try {
            log.info("orderStatusNotice发送kafka"+ JSONObject.toJSONString(req));
            String topic = "hltrip_order_orderstatus";
            OrderStatusKafka orderStatusKafka = new OrderStatusKafka();
            BeanUtils.copyProperties(req,orderStatusKafka);
            if(orderStatusKafka!=null){
                switch (orderStatusKafka.getOrderStatus()){
                    case 0:orderStatusKafka.setOrderStatus(OrderStatus.TO_BE_PAID.getCode());break;
                    case 10:orderStatusKafka.setOrderStatus(OrderStatus.PAYMENT_TO_BE_CONFIRMED.getCode());break;
                    case 11:orderStatusKafka.setOrderStatus(OrderStatus.TO_BE_CONFIRMED.getCode());break;
                    case 12:orderStatusKafka.setOrderStatus(OrderStatus.WAITING_APPOINTMENT.getCode());break;
                    case 13:orderStatusKafka.setOrderStatus(OrderStatus.TO_PAID_TWICE.getCode());break;
                    case 20:orderStatusKafka.setOrderStatus(OrderStatus.WAITING_TO_TRAVEL.getCode());break;
                    case 30:orderStatusKafka.setOrderStatus(OrderStatus.CONSUMED.getCode());break;
                    case 40:orderStatusKafka.setOrderStatus(OrderStatus.CANCELLED.getCode());break;
                    default : log.error("订单状态错误  推送的状态是 ：{}",orderStatusKafka.getOrderStatus()); break;
                }
            }
            ListenableFuture<SendResult<String, String>> listenableFuture = kafkaTemplate.send(topic, JSONObject.toJSONString(orderStatusKafka));
            listenableFuture.addCallback(
                    result -> log.info("订单状态推送kafka成功, params : {}", JSONObject.toJSONString(req)),
                    ex -> {
                        log.info("订单状态推送kafka失败, error message:{}", ex.getMessage(), ex);
                    });
        } catch (Exception e) {
            log.info("",e);
        }
    }

    @Override
    public BaseResponse<CenterPayCheckRes> payCheck(PayOrderReq req) {
        OrderManager orderManager = orderFactory.getOrderManager(req.getChannelCode());
        //校验manager处理
        checkManger(orderManager);
        //todo 支付前校验逻辑
        BaseResponse<CenterPayCheckRes> result = orderManager.payCheck(req);
        return result;
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
