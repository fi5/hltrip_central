package com.huoli.trip.central.web.converter;

import com.huoli.trip.common.constant.OrderStatus;
import com.huoli.trip.common.vo.request.PayOrderReq;
import com.huoli.trip.common.vo.response.order.CenterPayOrderRes;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfPayOrderReq;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfPayOrderRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 描述: <br> 支付订单转换
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：王德铭<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/5<br>
 */
@Component
@Slf4j
public class PayOrderConverter implements Converter<PayOrderReq, YcfPayOrderReq, YcfPayOrderRes, CenterPayOrderRes> {
    @Override
    public YcfPayOrderReq convertRequestToSupplierRequest(PayOrderReq req) {
        if(req == null){
            return null;
        }
        YcfPayOrderReq ycfPayOrderReq = new YcfPayOrderReq();
        //组装支付流水参数
        ycfPayOrderReq.setPaySerialNumber(req.getPaySerialNumber());
        ycfPayOrderReq.setPartnerOrderId(req.getPartnerOrderId());
        ycfPayOrderReq.setPrice(req.getPrice());
        return ycfPayOrderReq;
    }

    @Override
    public CenterPayOrderRes convertSupplierResponseToResponse(YcfPayOrderRes supplierResponse) {
        if(supplierResponse == null){
            return null;
        }
        CenterPayOrderRes payOrderRes = new CenterPayOrderRes();
        payOrderRes.setChannelOrderId(supplierResponse.getOrderId());
        switch (supplierResponse.getOrderStatus()){
            case 0:payOrderRes.setOrderStatus(OrderStatus.TO_BE_PAID.getCode());break;
            case 10:payOrderRes.setOrderStatus(OrderStatus.PAYMENT_TO_BE_CONFIRMED.getCode());break;
            case 11:payOrderRes.setOrderStatus(OrderStatus.TO_BE_CONFIRMED.getCode());break;
            case 12:payOrderRes.setOrderStatus(OrderStatus.WAITING_APPOINTMENT.getCode());break;
            case 13:payOrderRes.setOrderStatus(OrderStatus.TO_PAID_TWICE.getCode());break;
            case 20:payOrderRes.setOrderStatus(OrderStatus.WAITING_TO_TRAVEL.getCode());break;
            case 30:payOrderRes.setOrderStatus(OrderStatus.CONSUMED.getCode());break;
            case 40:payOrderRes.setOrderStatus(OrderStatus.CANCELLED.getCode());break;
            default : log.info("支付订单服务 订单状态返回异类 ：{}",supplierResponse.getOrderStatus()); break;
        }
        return payOrderRes;
    }
}
