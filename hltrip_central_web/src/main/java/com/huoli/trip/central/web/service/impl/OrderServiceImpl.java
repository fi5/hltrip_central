package com.huoli.trip.central.web.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.huoli.trip.central.api.OrderService;
import com.huoli.trip.central.web.converter.OrderInfoTranser;
import com.huoli.trip.central.web.service.OrderFactory;
import com.huoli.trip.central.web.util.TraceIdUtils;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.constant.Constants;
import com.huoli.trip.common.vo.TripNotice;
import com.huoli.trip.common.vo.request.*;
import com.huoli.trip.common.vo.request.central.CenterRefundCheckRequest;
import com.huoli.trip.common.vo.request.central.OrderStatusKafka;
import com.huoli.trip.common.vo.request.central.RefundKafka;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.central.CenterRefundCheckResult;
import com.huoli.trip.common.vo.response.order.*;
import com.huoli.trip.supplier.api.ProductService;
import com.huoli.trip.supplier.api.YcfOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.annotation.Service;
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
    @Reference(group = "hltrip",timeout = 30000,check=false,retries = 3)
    ProductService productService;
    @Reference(timeout = 10000,group = "hltrip")
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
        String channelCode = req.getChannelCode();
       /* if(channelCode.startsWith("hllx")){
            channelCode = "hllx";
        }*/
        OrderManager orderManager =orderFactory.getOrderManager(channelCode);
        log.info("获取到的 orderManager is：{}", JSON.toJSONString(orderManager));
        //校验manager处理
        checkManger(orderManager);
        //封装中台返回
        BaseResponse<CenterBookCheck> checkRes = orderManager.getCenterCheckInfos(req);
        if(checkRes != null && checkRes.getCode() != 0) {
            log.info("预订前校验失败,唤起本地更新产品为下线状态");
            try{
                if(StringUtils.isBlank(req.getCategory())){
                    productService.updateStatusByCode(req.getProductId(), Constants.PRODUCT_STATUS_INVALID);
                } else {
                    productService.updateStatusByCodev2(req.getProductId(), Constants.PRODUCT_STATUS_INVALID,req.getCategory());
                }
            }catch (Exception ex){
                log.error("调用下线本地数据接口失败");
            }
        }
        return checkRes;
    }

    @Override
    public BaseResponse refundNotice(RefundNoticeReq req) {

        try {
            log.info("refundNotice发送kafka"+ JSONObject.toJSONString(req));
            String topic = Constants.REFUND_ORDER_TOPIC;
            RefundKafka kafkaInfo = new RefundKafka();
            kafkaInfo.setOrderId(req.getPartnerOrderId());
            kafkaInfo.setRefundStatus(req.getRefundStatus());
            kafkaInfo.setRefundPrice(req.getRefundPrice());
            kafkaInfo.setExpense(req.getRefundCharge());
            kafkaInfo.setHandleRemark(req.getHandleRemark());
            kafkaInfo.setRefundReason(req.getRefundReason());
            kafkaInfo.setTraceId(TraceIdUtils.getTraceId());
//            log.info("这里的kafkaInfo:"+JSONObject.toJSONString(kafkaInfo));
            if(null!=req.getRefundTime())
            kafkaInfo.setRefundTime(req.getRefundTime());
            kafkaInfo.setResponseTime(req.getResponseTime());
            kafkaInfo.setSupplierRefused(req.getSupplierRefused());
            ListenableFuture<SendResult<String, String>> listenableFuture = kafkaTemplate.send(topic, JSONObject.toJSONString(kafkaInfo));
            listenableFuture.addCallback(
                    result -> log.info("订单发送kafka成功, params : {}", JSONObject.toJSONString(kafkaInfo)),
                    ex -> {
                        log.info("订单发送kafka失败, error message:{}", ex.getMessage(), ex);
                    });
            return BaseResponse.success(null);
        } catch (Exception e) {
        	log.error("refundNotice写kafka时报错:"+JSONObject.toJSONString(req),e);
            return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
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
        	log.error("getOrder查询订单报错",e);
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
        OrderManager orderManager = orderFactory.getOrderManager(req.getChannelCode());
        //校验manager处理
        checkManger(orderManager);

        BaseResponse<CenterCreateOrderRes> result = orderManager.getCenterCreateOrder(req);
        if(result != null && result.getCode() != 0) {
            log.info("供应商创建订单失败,唤起本地更新产品为下线状态");
            try{
                productService.updateStatusByCode(req.getProductId(), Constants.PRODUCT_STATUS_INVALID);
            }catch (Exception ex){
                log.error("调用下线本地数据接口失败");
            }
        }
        return result;
    }

    @Override
    public BaseResponse<CenterPayOrderRes> payOrder(PayOrderReq req) {
        OrderManager orderManager = orderFactory.getOrderManager(req.getChannelCode());
        //校验manager处理
        checkManger(orderManager);
        BaseResponse<CenterPayOrderRes> result = orderManager.getCenterPayOrder(req);
        return result;
    }

    @Override
    public BaseResponse<CenterCancelOrderRes> cancelOrder(CancelOrderReq req) {
        OrderManager orderManager = orderFactory.getOrderManager(req.getChannelCode());
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
//            log.info("orderStatusNotice发送kafka"+ JSONObject.toJSONString(req));
            String topic = Constants.HLTRIP_ORDER_ORDERSTATUS;
            OrderStatusKafka orderStatusKafka = new OrderStatusKafka();
            BeanUtils.copyProperties(req,orderStatusKafka);
            orderStatusKafka.setTraceId(TraceIdUtils.getTraceId());
            //订单状态转换下推送
            orderStatusKafka.setOrderStatus(OrderInfoTranser.genCommonOrderStringStatus(req.getStrStatus(),req.getType()));
            ListenableFuture<SendResult<String, String>> listenableFuture = kafkaTemplate.send(topic, JSONObject.toJSONString(orderStatusKafka));
            listenableFuture.addCallback(
                    result -> log.info("订单状态推送kafka成功, params : {}", JSONObject.toJSONString(orderStatusKafka)),
                    ex -> {
                        log.info("订单状态推送kafka失败, error message:{}", ex);
                    });
        } catch (Exception e) {
            log.error("订单状态推送kafka失败 发送的 json:{} 失败原因:{}",JSONObject.toJSONString(req),e);
        }
    }

    @Override
    public void tripNotice(TripNotice tripNotice) {
        try {
            String topic = Constants.HLTRIP_ORDER_TRIP_NOTICE;
            //订单状态转换下推送
            ListenableFuture<SendResult<String, String>> listenableFuture = kafkaTemplate.send(topic, JSONObject.toJSONString(tripNotice));
            listenableFuture.addCallback(
                    result -> log.info("出团通知推送kafka成功, params : {}", JSONObject.toJSONString(tripNotice)),
                    ex -> log.info("出团通知推送kafka失败, error message:{}", ex));
        } catch (Exception e) {
            log.error("出团通知推送kafka失败 发送的 json:{} 失败原因:{}",JSONObject.toJSONString(tripNotice), e);
        }
    }

    @Override
    public BaseResponse<CenterPayCheckRes> payCheck(PayOrderReq req) {
        OrderManager orderManager = orderFactory.getOrderManager(req.getChannelCode());
        //校验manager处理
        checkManger(orderManager);
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
            return BaseResponse.fail(CentralError.NO_RESULT_ERROR);
        }
        return manager;
    }

    @Override
    public BaseResponse<CenterRefundCheckResult> refundCheck(CenterRefundCheckRequest request) {
        OrderManager orderManager = orderFactory.getOrderManager(request.getChannelCode());
        //校验manager处理
        checkManger(orderManager);
        BaseResponse<CenterRefundCheckResult> result = orderManager.refundCheck(request);
        return result;
    }
}
