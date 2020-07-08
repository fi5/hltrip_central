package com.huoli.trip.central.web.converter;

import com.huoli.trip.common.vo.request.CancelOrderReq;
import com.huoli.trip.common.vo.response.order.CenterCancelOrderRes;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfCancelOrderReq;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfCancelOrderRes;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

/**
 * 描述: <br>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：王德铭<br>
 * 版本：1.0<br>
 * 创建日期：2020/4/26<br>
 */
@Component
public class CancelOrderConverter implements Converter<CancelOrderReq, YcfCancelOrderReq, YcfCancelOrderRes, CenterCancelOrderRes> {
    @Override
    public YcfCancelOrderReq convertRequestToSupplierRequest(CancelOrderReq req) {
        YcfCancelOrderReq ycfCancelOrderReq = new YcfCancelOrderReq();
        ycfCancelOrderReq.setPartnerOrderId(req.getPartnerOrderId());
        ycfCancelOrderReq.setRemark(req.getRemark());
        return ycfCancelOrderReq;
    }

    @Override
    public CenterCancelOrderRes convertSupplierResponseToResponse(YcfCancelOrderRes supplierResponse) {
        CenterCancelOrderRes cancelOrderRes = new CenterCancelOrderRes();
        cancelOrderRes.setAsync(supplierResponse.getAsync());
        cancelOrderRes.setOrderStatus(supplierResponse.getOrderStatus());
        return cancelOrderRes;
    }
}
