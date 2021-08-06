package com.huoli.trip.central.web.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.huoli.trip.central.api.ProductService;
import com.huoli.trip.central.web.converter.OrderInfoTranser;
import com.huoli.trip.central.web.converter.SupplierErrorMsgTransfer;
import com.huoli.trip.central.web.dao.ScenicSpotDao;
import com.huoli.trip.central.web.util.TraceIdUtils;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.constant.Certificate;
import com.huoli.trip.common.constant.ChannelConstant;
import com.huoli.trip.common.constant.OrderStatus;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.ScenicSpotProductMPO;
import com.huoli.trip.common.exception.HlCentralException;
import com.huoli.trip.common.util.DateTimeUtil;
import com.huoli.trip.common.util.ListUtils;
import com.huoli.trip.common.vo.request.*;
import com.huoli.trip.common.vo.request.central.PriceCalcRequest;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.central.PriceCalcResult;
import com.huoli.trip.common.vo.response.order.*;
import com.huoli.trip.supplier.api.UBROrderService;
import com.huoli.trip.supplier.self.difengyun.DfyOrderDetail;
import com.huoli.trip.supplier.self.difengyun.vo.DfyBookSaleInfo;
import com.huoli.trip.supplier.self.difengyun.vo.request.*;
import com.huoli.trip.supplier.self.difengyun.vo.response.DfyBaseResult;
import com.huoli.trip.supplier.self.difengyun.vo.response.DfyBookCheckResponse;
import com.huoli.trip.supplier.self.difengyun.vo.response.DfyOrderStatusResponse;
import com.huoli.trip.supplier.self.difengyun.vo.response.DfyRefundTicketResponse;
import com.huoli.trip.supplier.self.universal.vo.UBRGuest;
import com.huoli.trip.supplier.self.universal.vo.UBROrderDetailResponse;
import com.huoli.trip.supplier.self.universal.vo.UBRTicketEntity;
import com.huoli.trip.supplier.self.universal.vo.reqeust.UBRTicketOrderLocalRequest;
import com.huoli.trip.supplier.self.universal.vo.reqeust.UBRTicketOrderRequest;
import com.huoli.trip.supplier.self.universal.vo.response.UBRBaseResponse;
import com.huoli.trip.supplier.self.universal.vo.response.UBRTicketOrderResponse;
import com.huoli.trip.supplier.self.yaochufa.vo.BaseOrderRequest;
import io.netty.util.internal.UnstableApi;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2021/8/3<br>
 */
@Component
@Slf4j
public class BtgOrderManager extends OrderManager {

    @Reference(timeout = 10000,group = "hltrip", check = false)
    private UBROrderService ubrOrderService;

    @Autowired
    private ScenicSpotDao scenicSpotDao;

    @Autowired
    private ProductService productService;

    public final static String CHANNEL = ChannelConstant.SUPPLIER_TYPE_BTG;

    public String getChannel(){
        return CHANNEL;
    }


    /**
     * 创建订单
     * @param req
     * @return
     */
    public BaseResponse<CenterCreateOrderRes> getCenterCreateOrder(CreateOrderReq req){

        UBRTicketOrderRequest orderRequest = new UBRTicketOrderRequest();
        UBRGuest ubrContact = new UBRGuest();
        ubrContact.setName(req.getCname());
        ubrContact.setTelephone(req.getMobile());
        orderRequest.setContact(ubrContact);
        ScenicSpotProductMPO scenicSpotProductMPO = scenicSpotDao.querySpotProductById(req.getProductId());
        UBRTicketEntity ticketEntity = new UBRTicketEntity();
        ticketEntity.setCode(scenicSpotProductMPO.getSupplierProductId());
        ticketEntity.setDatetime(String.format("%sT08:00:00", req.getBeginDate()));
        ticketEntity.setPrice(req.getSellAmount() != null ? req.getSellAmount().toPlainString() : null);
        List<UBRGuest> ubrGuests = req.getGuests().stream().map(g -> {
            UBRGuest ubrGuest = new UBRGuest();
            ubrGuest.setTelephone(g.getMobile());
            ubrGuest.setName(g.getCname());
            ubrGuest.setIdNo(g.getCredential());
            if(g.getCredentialType() == Certificate.ID_CARD.getCode()){
                ubrGuest.setIdType("GovernmentId");
            } else if(g.getCredentialType() == Certificate.PASSPORT.getCode()){
                ubrGuest.setIdType("Passport");
            }
            return ubrGuest;
        }).collect(Collectors.toList());
        ticketEntity.setGuest(ubrGuests);
        orderRequest.setTicketEntity(Lists.newArrayList(ticketEntity));
        String traceId = req.getTraceId();
        if(org.apache.commons.lang3.StringUtils.isEmpty(traceId)){
            traceId = TraceIdUtils.getTraceId();
        }
        orderRequest.setTraceId(traceId);
        CenterCreateOrderRes createOrderRes = new CenterCreateOrderRes();
        // 这里还没真下单。没有渠道订单号
//            createOrderRes.setOrderId(order.getData().getOrderId());
        createOrderRes.setOrderStatus(OrderStatus.TO_BE_PAID.getCode());
        createOrderRes.setOrderExtend(JSONObject.toJSONString(orderRequest));
        return BaseResponse.success(createOrderRes);
    }

    public BaseResponse<CenterBookCheck> getCenterCheckInfos(BookCheckReq req){
        log.info("进入可预订检查 btg");
        String begin = req.getBeginDate();
        String end = req.getEndDate();
        if(StringUtils.isEmpty(end)){
            end = begin;
        }
        // 价格计算需要重新调
        CenterBookCheck  bookCheck = new CenterBookCheck();
        PriceCalcRequest calcRequest = new PriceCalcRequest();
        calcRequest.setStartDate(DateTimeUtil.parseDate(begin));
        calcRequest.setEndDate(DateTimeUtil.parseDate(end));
        calcRequest.setProductCode(req.getProductId());
        calcRequest.setQuantity(req.getCount());
        //2021-06-02
        calcRequest.setChannelCode(req.getChannelCode());
        calcRequest.setFrom(req.getFrom());
        calcRequest.setPackageCode(req.getPackageId());
        calcRequest.setCategory(req.getCategory());
        PriceCalcResult priceCalcResult;
        String traceId = req.getTraceId();
        if(org.apache.commons.lang3.StringUtils.isEmpty(traceId)){
            traceId = TraceIdUtils.getTraceId();
        }
        calcRequest.setTraceId(traceId);
        try{
            BaseResponse<PriceCalcResult> priceCalcResultBaseResponse;
            if(StringUtils.isBlank(req.getCategory())){
                priceCalcResultBaseResponse = productService.calcTotalPrice(calcRequest);
            } else {
                priceCalcResultBaseResponse = productService.calcTotalPriceV2(calcRequest);
            }
            priceCalcResult = priceCalcResultBaseResponse.getData();
            //没有价格直接抛异常
            if(priceCalcResultBaseResponse.getCode()!=0||priceCalcResult==null){
                return BaseResponse.fail(CentralError.PRICE_CALC_PRICE_NOT_FOUND_ERROR);
            }
        }catch (HlCentralException e){
            log.error("价格计算失败。", e);
            return BaseResponse.fail(CentralError.PRICE_CALC_PRICE_NOT_FOUND_ERROR);
        }
        //设置结算总价
        bookCheck.setSettlePrice(priceCalcResult.getSettlesTotal());
        //销售总价
        bookCheck.setSalePrice(priceCalcResult.getSalesTotal());
        return BaseResponse.success(bookCheck);
    }

    public BaseResponse<OrderDetailRep> getOrderDetail(OrderOperReq req){
        BaseOrderRequest baseOrderRequest = new BaseOrderRequest();
        baseOrderRequest.setOrderId(req.getOrderId());
        String traceId = req.getTraceId();
        if(org.apache.commons.lang3.StringUtils.isEmpty(traceId)){
            traceId = TraceIdUtils.getTraceId();
        }
        baseOrderRequest.setTraceId(traceId);
        baseOrderRequest.setSupplierOrderId(req.getSupplierOrderId());
        final UBRBaseResponse<UBROrderDetailResponse> order = ubrOrderService.orderDetail(baseOrderRequest);
        try {
            log.info("btg 拿到的订单数据:" + JSONObject.toJSONString(order));
            UBROrderDetailResponse orderDetail = order.getData();
            //如果数据为空,直接返回错
            if(order == null || order.getCode() != 200 || order.getData() == null)
                return BaseResponse.fail(CentralError.ERROR_NO_ORDER);//异常消息以供应商返回的
            OrderDetailRep rep=new OrderDetailRep();
            rep.setOrderId(orderDetail.getOrderUid());
            //转换成consumer统一的订单状态
            rep.setOrderStatus(OrderInfoTranser.genCommonOrderStringStatus(orderDetail.getStatus(),6));
//            rep.setVochers(genVouchers(dfyOrderDetail));
            return BaseResponse.success(rep);
        } catch (Exception e) {
            log.error("btg订单详情异常，",e);
            return BaseResponse.fail(9999, order.getMsg(),null);
        }
    }

    /**
     * 支付订单
     * @param req
     * @return
     */
    public BaseResponse<CenterPayOrderRes> getCenterPayOrder(PayOrderReq req){
        BaseOrderRequest request = new BaseOrderRequest();
        request.setOrderId(req.getPartnerOrderId());
        request.setSupplierOrderId(req.getChannelOrderId());
        request.setTraceId(req.getTraceId());
        UBRBaseResponse<UBRTicketOrderResponse> ubrBaseResponse = ubrOrderService.payOrder(request);
        if(ubrBaseResponse != null && ubrBaseResponse.getCode() == 200 && ubrBaseResponse.getData() != null){
            CenterPayOrderRes payOrderRes = new CenterPayOrderRes();
            payOrderRes.setChannelOrderId(ubrBaseResponse.getData().getOrderId());
            payOrderRes.setLocalOrderId(req.getPartnerOrderId());
            payOrderRes.setOrderStatus(10);
            return BaseResponse.success(payOrderRes);
        }
        return BaseResponse.fail(CentralError.ERROR_ORDER_PAY);
    }

    /**
     * 申请退款
     * @param req
     * @return
     */
    public BaseResponse<CenterCancelOrderRes> getCenterApplyRefund(CancelOrderReq req){
        BaseOrderRequest orderRequest = new BaseOrderRequest();
        orderRequest.setTraceId(req.getTraceId());
        orderRequest.setOrderId(req.getPartnerOrderId());
        orderRequest.setSupplierOrderId(req.getOutOrderId());
        UBRBaseResponse<UBRTicketOrderResponse> baseResponse = ubrOrderService.refund(orderRequest);
        if(baseResponse != null && baseResponse.getCode() == 200){
            CenterCancelOrderRes centerCancelOrderRes = new CenterCancelOrderRes();
            centerCancelOrderRes.setOrderStatus(OrderStatus.APPLYING_FOR_REFUND.getCode());
            return BaseResponse.success(centerCancelOrderRes);
        }
        return BaseResponse.fail(CentralError.ERROR_SUPPLIER_APPLYREFUND_ORDER);
    }

    /**
     * 支付前校验
     * @param req
     * @return
     */
    public  BaseResponse<CenterPayCheckRes> payCheck(PayOrderReq req){
        CenterPayCheckRes  payCheckRes = new CenterPayCheckRes();
        payCheckRes.setResult(true);
        return BaseResponse.success(payCheckRes);
    }

    /**
     * 取消订单
     * @param req
     * @return
     */
    public  BaseResponse<CenterCancelOrderRes> getCenterCancelOrder(CancelOrderReq req){
        CenterCancelOrderRes centerCancelOrderRes = new CenterCancelOrderRes();
        centerCancelOrderRes.setOrderStatus(OrderStatus.CANCELLED.getCode());
        return BaseResponse.success(centerCancelOrderRes);
    }

}
