package com.huoli.trip.central.api;

import com.huoli.trip.common.vo.request.BookCheckReq;
import com.huoli.trip.common.vo.request.OrderStatusRequest;
import com.huoli.trip.common.vo.request.RefundNoticeReq;

/**
 * 中台订单相关dubbo服务接口定义
 */
public interface OrderService {
    Object getOrderStatus(OrderStatusRequest request);

    /**
     * 描述: <br> 获取订单可预定信息
     * 版权：Copyright (c) 2011-2020<br>
     * 公司：活力天汇<br>
     * 作者：王德铭<br>
     * 版本：1.0<br>
     * 创建日期：2020/7/1<br>
     */
    Object getCheckInfos(BookCheckReq req) throws Exception;

    void refundNotice(RefundNoticeReq req) ;



}
