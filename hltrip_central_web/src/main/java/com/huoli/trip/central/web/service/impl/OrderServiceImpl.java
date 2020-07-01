package com.huoli.trip.central.web.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.huoli.trip.central.api.OrderService;
import com.huoli.trip.common.vo.request.BookCheckReq;
import com.huoli.trip.common.vo.request.OrderStatusRequest;
import com.huoli.trip.common.vo.request.RefundNoticeReq;
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
public class OrderServiceImpl implements OrderService {


    @Autowired
    KafkaTemplate kafkaTemplate;


    @Override
    public Object getOrderStatus(OrderStatusRequest request) {
        return null;
    }

    @Override
    public Object getCheckInfos(BookCheckReq req) {
        return null;
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
}
