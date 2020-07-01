package com.huoli.trip.central.api;

import com.huoli.trip.common.vo.request.BookCheckReq;
import com.huoli.trip.common.vo.request.OrderStatusRequest;
import com.huoli.trip.common.vo.request.RefundNoticeReq;
import com.huoli.trip.common.vo.response.BaseResponse;

/**
 * 中台订单相关dubbo服务接口定义
 */
public interface OrderService {
    Object getOrderStatus(OrderStatusRequest request);
    Object getCheckInfos(BookCheckReq req);

    void refundNotice(RefundNoticeReq req) ;



}
