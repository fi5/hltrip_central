package com.huoli.trip.central.api;

import com.huoli.trip.common.vo.request.BookCheckReq;
import com.huoli.trip.common.vo.request.OrderOperReq;
import com.huoli.trip.common.vo.request.OrderStatusRequest;
import com.huoli.trip.common.vo.request.RefundNoticeReq;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.order.OrderDetailRep;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfOrderStatusResult;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfVouchersResult;
import com.huoli.trip.supplier.self.yaochufa.vo.basevo.YcfBaseResult;

/**
 * 中台订单相关dubbo服务接口定义
 */
public interface OrderService {
    Object getOrderStatus(OrderStatusRequest request);
    Object getCheckInfos(BookCheckReq req);

    void refundNotice(RefundNoticeReq req) ;

    BaseResponse<OrderDetailRep> getOrder(OrderOperReq req);

    /**
     * 重新获取凭证
     */
    BaseResponse<OrderDetailRep> getVochers(OrderOperReq req);



}
