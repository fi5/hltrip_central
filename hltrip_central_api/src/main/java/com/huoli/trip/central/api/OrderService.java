package com.huoli.trip.central.api;

import com.huoli.trip.common.vo.request.*;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.order.CenterBookCheckRes;
import com.huoli.trip.common.vo.response.order.CenterCreateOrderRes;
import com.huoli.trip.common.vo.response.order.OrderDetailRep;
import com.huoli.trip.common.vo.response.order.CenterPayOrderRes;

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
    BaseResponse<CenterBookCheckRes> getCheckInfos(BookCheckReq req);

    void refundNotice(RefundNoticeReq req) ;

    BaseResponse<OrderDetailRep> getOrder(OrderOperReq req);

    /**
     * 重新获取凭证
     */
    BaseResponse<OrderDetailRep> getVochers(OrderOperReq req);

    /**
     * 描述: <br> 创建订单
     * 版权：Copyright (c) 2011-2020<br>
     * 公司：活力天汇<br>
     * 作者：王德铭<br>
     * 版本：1.0<br>
     * 创建日期：2020/7/2<br>
     */
    BaseResponse<CenterCreateOrderRes> createOrder(CreateOrderReq req);
    /**
     * 描述: <br> 支付订单
     * 版权：Copyright (c) 2011-2020<br>
     * 公司：活力天汇<br>
     * 作者：王德铭<br>
     * 版本：1.0<br>
     * 创建日期：2020/7/2<br>
     */
    BaseResponse<CenterPayOrderRes> payOrder(PayOrderReq req);



}
