package com.huoli.trip.central.web.converter;

import com.huoli.trip.common.vo.request.CancelOrderReq;
import com.huoli.trip.common.vo.response.order.CenterCancelOrderRes;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfCancelOrderReq;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfCancelOrderRes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 描述: <br> 取消订单转换
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：王德铭<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/3<br>
 */
@Component
@Slf4j
public class CancelOrderConverter implements Converter<CancelOrderReq, YcfCancelOrderReq, YcfCancelOrderRes, CenterCancelOrderRes> {
    @Override
    public YcfCancelOrderReq convertRequestToSupplierRequest(CancelOrderReq req) {
        if(req == null){
            return null;
        }
        YcfCancelOrderReq ycfCancelOrderReq = new YcfCancelOrderReq();
        ycfCancelOrderReq.setPartnerOrderId(req.getPartnerOrderId());
        ycfCancelOrderReq.setRemark(req.getRemark());
        ycfCancelOrderReq.setTraceId(req.getTraceId());
        return ycfCancelOrderReq;
    }

    /*
    0	待支付：创建订单成功，合作方尚未付款。
10	待确认：支付订单成功，要出发确认流程中
11	待确认（申请取消）：合作方申请取消，要出发在审核状态
12	[全网预售特有]预约出行：待二次预约
13	[全网预售特有]立即补款：待二次预约补款
20	待出行：要出发已确认订单，客人可出行消费
30	已消费：客人已消费订单
40	已取消：订单已取消
     */
    @Override
    public CenterCancelOrderRes convertSupplierResponseToResponse(YcfCancelOrderRes supplierResponse) {
        if(supplierResponse == null){
            return null;
        }
        CenterCancelOrderRes cancelOrderRes = new CenterCancelOrderRes();
        cancelOrderRes.setOrderStatus(OrderInfoTranser.genCommonOrderStatus(supplierResponse.getOrderStatus(),1));
        return cancelOrderRes;
    }
}
