package com.huoli.trip.central.web.service.impl;

import com.huoli.trip.central.web.converter.OrderInfoTranser;
import com.huoli.trip.central.web.converter.SupplierErrorMsgTransfer;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.constant.ChannelConstant;
import com.huoli.trip.common.constant.OrderStatus;
import com.huoli.trip.common.exception.HlCentralException;
import com.huoli.trip.common.util.DateTimeUtil;
import com.huoli.trip.common.util.ListUtils;
import com.huoli.trip.common.vo.request.*;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.order.*;
import com.huoli.trip.supplier.api.HllxService;

import com.huoli.trip.supplier.self.hllx.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class HllxOrderManager extends OrderManager {

    @Reference(timeout = 10000, group = "hltrip", check = false)
    private HllxService hllxService;

    public final static String CHANNEL= ChannelConstant.SUPPLIER_TYPE_HLLX;
    public String getChannel(){
        return CHANNEL;
    }
    public String test() {
        System.out.println("hllx");
        return "hllx";
    }

    /**
     * 可预订检查
     * @param req
     * @return
     */
    public BaseResponse<CenterBookCheck> getCenterCheckInfos(BookCheckReq req){
        String begin = req.getBeginDate();
        String end = req.getEndDate();
        if(StringUtils.isEmpty(end)){
            end = begin;
        }

        /**
         * 开始日期大于结束日期
         */
        if(DateTimeUtil.parseDate(begin).after(DateTimeUtil.parseDate(begin))){
            return BaseResponse.fail(CentralError.ERROR_DATE_ORDER_1);
        }
        /**
         * 时间跨度大于90天
         */
        /*if(this.isOutTime(DateTimeUtil.parseDate(begin),DateTimeUtil.parseDate(begin))){
            return BaseResponse.fail(CentralError.ERROR_DATE_ORDER_2);
        }*/

        //开始组装供应商请求参数
        HllxBookCheckReq req1 = new HllxBookCheckReq();
        //转供应商productId
        //ycfBookCheckReq.setProductId(CentralUtils.getSupplierId(req.getProductId()));
        req1.setBeginDate(begin);
        req1.setEndDate(end);
        HllxBookCheckRes hllxBookCheckRes;
        try {
            //供应商输出
            HllxBaseResult<HllxBookCheckRes> checkInfos = hllxService.getCheckInfos(req1);
            if(checkInfos!=null&&checkInfos.getStatusCode()==200){
                hllxBookCheckRes = checkInfos.getData();
                if(hllxBookCheckRes == null){
                    return SupplierErrorMsgTransfer.buildMsg(checkInfos.getMessage());//异常消息以供应商返回的
                }else{
                    CenterBookCheck  bookCheck = new CenterBookCheck();
                    List<HllxBookSaleInfo> saleInfos = hllxBookCheckRes.getSaleInfos();
                    if(ListUtils.isNotEmpty(saleInfos)){
                        return BaseResponse.fail(CentralError.NO_STOCK_ERROR);
                    }
                    HllxBookSaleInfo hllxBookSaleInfo = saleInfos.get(0);
                    int stocks = hllxBookSaleInfo.getTotalStock();
                    if(req.getCount() > stocks){
                        return BaseResponse.withFail(CentralError.NOTENOUGH_STOCK_ERROR,bookCheck);
                    }
                    bookCheck.setSettlePrice(hllxBookSaleInfo.getPrice());
                    bookCheck.setSalePrice(hllxBookSaleInfo.getSalePrice());
                    bookCheck.setStock(stocks);
                    return BaseResponse.success(bookCheck);
                }
            }else{
                return BaseResponse.fail(CentralError.ERROR_SUPPLIER_BOOK_CHECK_ORDER);
            }
        }catch (HlCentralException e){
            return BaseResponse.fail(CentralError.ERROR_SUPPLIER_BOOK_CHECK_ORDER);
        }
    }

    /**
     * 创建订单
     * @param req
     * @return
     */
    public BaseResponse<CenterCreateOrderRes> getCenterCreateOrder(CreateOrderReq req){
        HllxCreateOrderReq hllxCreateOrderReq = new HllxCreateOrderReq();
        hllxCreateOrderReq.setDate(req.getBeginDate());
        hllxCreateOrderReq.setProductId(req.getProductId());
        hllxCreateOrderReq.setQunatity(req.getQunatity());
        HllxBaseResult<HllxCreateOrderRes> resHllxBaseResult = hllxService.createOrder(hllxCreateOrderReq);
        if(resHllxBaseResult != null && resHllxBaseResult.getSuccess() && resHllxBaseResult.getData() != null) {
            CenterCreateOrderRes createOrderRes = new CenterCreateOrderRes();
            //订单号怎么处理
            //createOrderRes.setOrderId(supplierResponse.getOrderId());
            createOrderRes.setOrderStatus(OrderStatus.TO_BE_PAID.getCode());
            return BaseResponse.success(createOrderRes);
        }
        return BaseResponse.fail(CentralError.ERROR_ORDER);
    }

    /**
     * 支付订单
     * @param req
     * @return
     */
    public BaseResponse<CenterPayOrderRes> getCenterPayOrder(PayOrderReq req){
        HllxPayOrderReq hllxPayOrderReq = new HllxPayOrderReq();
        HllxBaseResult<HllxPayOrderRes> resHllxBaseResult = hllxService.payOrder(hllxPayOrderReq);
        if(resHllxBaseResult != null && resHllxBaseResult.getSuccess()){
            CenterPayOrderRes payOrderRes = new CenterPayOrderRes();
            payOrderRes.setChannelOrderId(req.getPartnerOrderId());
            payOrderRes.setOrderStatus(10);
            return BaseResponse.success(payOrderRes);
        }
        return BaseResponse.fail(CentralError.ERROR_ORDER_PAY);
    }

    /**
     * 取消订单
     * @param req
     * @return
     */
    public  BaseResponse<CenterCancelOrderRes> getCenterCancelOrder(CancelOrderReq req){
        HllxCancelOrderReq hllxCancelOrderReq = new HllxCancelOrderReq();
        HllxBaseResult<HllxCancelOrderRes> hllxCancelOrder = hllxService.cancelOrder(hllxCancelOrderReq);
        if(hllxCancelOrder != null && hllxCancelOrder.getSuccess() && hllxCancelOrder.getData() != null){
            CenterCancelOrderRes centerCancelOrderRes = new CenterCancelOrderRes();
            centerCancelOrderRes.setOrderStatus(hllxCancelOrder.getData().getOrderStatus());
            return BaseResponse.success(centerCancelOrderRes);
        }
        return BaseResponse.fail(CentralError.ERROR_SUPPLIER_CANCEL_ORDER);
    }

    /**
     * 申请退款
     * @param req
     * @return
     */
    public BaseResponse<CenterCancelOrderRes> getCenterApplyRefund(CancelOrderReq req){
        HllxBaseResult<HllxOrderStatusResult> resultHllxBaseResult = hllxService.drawback(req.getPartnerOrderId());
        if(resultHllxBaseResult != null && resultHllxBaseResult.getSuccess()){
            CenterCancelOrderRes centerCancelOrderRes = new CenterCancelOrderRes();
            centerCancelOrderRes.setOrderStatus(OrderStatus.APPLYING_FOR_REFUND.getCode());
            return BaseResponse.success(centerCancelOrderRes);
        }
        return BaseResponse.fail(CentralError.ERROR_SUPPLIER_APPLYREFUND_ORDER);
    }


    /**
     * 获取订单
     * @param req
     * @return
     */
    public BaseResponse<OrderDetailRep> getOrderDetail(OrderOperReq req){
        HllxBaseResult<HllxOrderStatusResult>   resultHllxBaseResult = hllxService.getOrder(req.getOrderId());
        if(resultHllxBaseResult != null && resultHllxBaseResult.getSuccess()){
            OrderDetailRep rep=new OrderDetailRep();
            rep.setOrderId(req.getOrderId());
            //是否要查询订单的状态
            //rep.setOrderStatus();
            return BaseResponse.success(rep);
        }
        return BaseResponse.fail(9999,"查询订单失败",null);
    }


}
