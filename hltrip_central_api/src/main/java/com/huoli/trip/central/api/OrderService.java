package com.huoli.trip.central.api;

import com.huoli.trip.common.vo.TripNotice;
import com.huoli.trip.common.vo.request.*;
import com.huoli.trip.common.vo.request.central.CenterRefundCheckRequest;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.central.CenterRefundCheckResult;
import com.huoli.trip.common.vo.response.order.*;

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
    BaseResponse<CenterBookCheck> getCheckInfos(BookCheckReq req);

	/**
     * 接收退款通知
     * @param req
     */
    BaseResponse refundNotice(RefundNoticeReq req) ;

	/**
     * 查询供应商最新订单状态等
     * @param req
     * @return
     */
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

    /**
     * 描述: <br> 取消订单
     * 版权：Copyright (c) 2011-2020<br>
     * 公司：活力天汇<br>
     * 作者：王德铭<br>
     * 版本：1.0<br>
     * 创建日期：2020/7/6<br>
     */
    BaseResponse<CenterCancelOrderRes> cancelOrder(CancelOrderReq req);

    /**
     * 描述: <br> 申请退款
     * 版权：Copyright (c) 2011-2020<br>
     * 公司：活力天汇<br>
     * 作者：王德铭<br>
     * 版本：1.0<br>
     * 创建日期：2020/7/7<br>
     */
    BaseResponse<CenterCancelOrderRes> applyRefund(CancelOrderReq req);

    /**
     * 描述: <br> 推送订单状态
     * 版权：Copyright (c) 2011-2020<br>
     * 公司：活力天汇<br>
     * 作者：王德铭<br>
     * 版本：1.0<br>
     * 创建日期：2020/7/7<br>
     */
    void orderStatusNotice(PushOrderStatusReq req) ;

    /**
     * 出团通知
     * @param tripNotice
     */
    void tripNotice(TripNotice tripNotice);

    /**
     * 描述: <br> 支付前校验
     * 版权：Copyright (c) 2011-2020<br>
     * 公司：活力天汇<br>
     * 作者：王德铭<br>
     * 版本：1.0<br>
     * 创建日期：2020/7/8<br>
     */
    BaseResponse<CenterPayCheckRes> payCheck(PayOrderReq req) ;

    /**
     * 退款前检查
     * @param request
     * @return
     */
    BaseResponse<CenterRefundCheckResult> refundCheck(CenterRefundCheckRequest request);

}
