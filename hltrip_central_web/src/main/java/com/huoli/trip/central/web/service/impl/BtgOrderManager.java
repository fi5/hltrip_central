package com.huoli.trip.central.web.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.huoli.trip.central.api.ProductService;
import com.huoli.trip.central.web.converter.OrderInfoTranser;
import com.huoli.trip.central.web.dao.ScenicSpotDao;
import com.huoli.trip.central.web.dao.ScenicSpotProductPriceDao;
import com.huoli.trip.central.web.mapper.TripOrderMapper;
import com.huoli.trip.central.web.util.TraceIdUtils;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.constant.Certificate;
import com.huoli.trip.common.constant.ChannelConstant;
import com.huoli.trip.common.constant.OrderStatus;
import com.huoli.trip.common.entity.TripOrder;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.ScenicSpotProductMPO;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.ScenicSpotProductPriceMPO;
import com.huoli.trip.common.exception.HlCentralException;
import com.huoli.trip.common.util.DateTimeUtil;
import com.huoli.trip.common.vo.request.*;
import com.huoli.trip.common.vo.request.central.CenterRefundCheckRequest;
import com.huoli.trip.common.vo.request.central.PriceCalcRequest;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.central.CenterRefundCheckResult;
import com.huoli.trip.common.vo.response.central.PriceCalcResult;
import com.huoli.trip.common.vo.response.order.*;
import com.huoli.trip.supplier.api.UBROrderService;
import com.huoli.trip.supplier.api.UBRProductService;
import com.huoli.trip.supplier.self.universal.vo.UBRGuest;
import com.huoli.trip.supplier.self.universal.vo.UBROrderDetailResponse;
import com.huoli.trip.supplier.self.universal.vo.UBRTicketEntity;
import com.huoli.trip.supplier.self.universal.vo.reqeust.UBRTicketListRequest;
import com.huoli.trip.supplier.self.universal.vo.reqeust.UBRTicketOrderRequest;
import com.huoli.trip.supplier.self.universal.vo.response.UBRBaseResponse;
import com.huoli.trip.supplier.self.universal.vo.response.UBRRefundCheckResponseCustom;
import com.huoli.trip.supplier.self.universal.vo.response.UBRTicketOrderResponse;
import com.huoli.trip.supplier.self.yaochufa.vo.BaseOrderRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ?????????<br/>
 * ?????????Copyright (c) 2011-2020<br>
 * ?????????????????????<br>
 * ??????????????????<br>
 * ?????????1.0<br>
 * ???????????????2021/8/3<br>
 */
@Component
@Slf4j
public class BtgOrderManager extends OrderManager {

    @Reference(timeout = 10000,group = "hltrip", check = false)
    private UBROrderService ubrOrderService;

    @Reference(timeout = 10000,group = "hltrip", check = false)
    private UBRProductService ubrProductService;

    @Autowired
    private ScenicSpotDao scenicSpotDao;

    @Autowired
    private ScenicSpotProductPriceDao scenicSpotProductPriceDao;

    @Autowired
    private ProductService productService;

    @Autowired
    private TripOrderMapper tripOrderMapper;

    public final static String CHANNEL = ChannelConstant.SUPPLIER_TYPE_BTG;

    public String getChannel(){
        return CHANNEL;
    }


    /**
     * ????????????
     * @param req
     * @return
     */
    public BaseResponse<CenterCreateOrderRes> getCenterCreateOrder(CreateOrderReq req){

        UBRTicketOrderRequest orderRequest = new UBRTicketOrderRequest();
        UBRGuest ubrContact = new UBRGuest();
        ubrContact.setName(req.getCname());
        ubrContact.setTelephone(req.getMobile());
        orderRequest.setContact(ubrContact);
        ScenicSpotProductMPO scenicSpotProductMPO = scenicSpotDao.querySpotProductById(req.getProductId(), null);
        String date = String.format("%sT06:00:00", DateTimeUtil.formatDate(DateTimeUtil.trancateToDate(DateTimeUtil.parseDate(req.getBeginDate()))));
        String oriPrice = null;
        if(StringUtils.isNotBlank(req.getPackageId())) {
            ScenicSpotProductPriceMPO priceMPO = scenicSpotProductPriceDao.getPriceById(req.getPackageId());
            if (priceMPO != null) {
                if(StringUtils.isNotBlank(priceMPO.getOriDate())){
                    date = priceMPO.getOriDate().replace(" ", "T");
                }
                oriPrice = priceMPO.getOriPrice();
            }
        }
        if(StringUtils.isBlank(oriPrice)){
            oriPrice = req.getOutPayUnitPrice() != null ? req.getOutPayUnitPrice().toPlainString() : null;
        }
        UBRTicketEntity ticketEntity = new UBRTicketEntity();
        ticketEntity.setCode(scenicSpotProductMPO.getSupplierProductId());
        ticketEntity.setDatetime(date);
        ticketEntity.setPrice(oriPrice);
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
        // ?????????????????????????????????????????????
//            createOrderRes.setOrderId(order.getData().getOrderId());
        createOrderRes.setOrderStatus(OrderStatus.TO_BE_PAID.getCode());
        createOrderRes.setOrderExtend(JSONObject.toJSONString(orderRequest));
        return BaseResponse.success(createOrderRes);
    }

    public BaseResponse<CenterBookCheck> getCenterCheckInfos(BookCheckReq req){
        log.info("????????????????????? btg");
        String begin = req.getBeginDate();
        String end = req.getEndDate();
        if(StringUtils.isEmpty(end)){
            end = begin;
        }
        // ???????????????????????????
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
        calcRequest.setSource(req.getSource());
        try{
            BaseResponse<PriceCalcResult> priceCalcResultBaseResponse;
            if(StringUtils.isBlank(req.getCategory())){
                priceCalcResultBaseResponse = productService.calcTotalPrice(calcRequest);
            } else {
                priceCalcResultBaseResponse = productService.calcTotalPriceV2(calcRequest);
            }
            priceCalcResult = priceCalcResultBaseResponse.getData();
            //???????????????????????????
            if(priceCalcResultBaseResponse.getCode()!=0||priceCalcResult==null){
                return BaseResponse.fail(CentralError.PRICE_CALC_PRICE_NOT_FOUND_ERROR);
            }
        }catch (HlCentralException e){
            log.error("?????????????????????", e);
            return BaseResponse.fail(CentralError.PRICE_CALC_PRICE_NOT_FOUND_ERROR);
        }
        CenterBookCheck  bookCheck = new CenterBookCheck();
        //??????????????????
        bookCheck.setSettlePrice(priceCalcResult.getSettlesTotal());
        //????????????
        bookCheck.setSalePrice(priceCalcResult.getSalesTotal());
        bookCheck.setStock(priceCalcResult.getStock() == null ? 0 : priceCalcResult.getStock());
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
            log.info("btg ?????????????????????:" + JSONObject.toJSONString(order));
            UBROrderDetailResponse orderDetail = order.getData();
            //??????????????????,???????????????
            if(order == null || order.getCode() != 200 || order.getData() == null)
                return BaseResponse.fail(CentralError.ERROR_NO_ORDER);//?????????????????????????????????
            OrderDetailRep rep=new OrderDetailRep();
            rep.setOrderId(orderDetail.getOrderUid());
            //?????????consumer?????????????????????
            rep.setOrderStatus(OrderInfoTranser.genCommonOrderStringStatus(orderDetail.getStatus(),6));
//            rep.setVochers(genVouchers(dfyOrderDetail));
            return BaseResponse.success(rep);
        } catch (Exception e) {
            log.error("btg?????????????????????",e);
            return BaseResponse.fail(9999, order.getMsg(),null);
        }
    }

    /**
     * ????????????
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
     * ????????????
     * @param req
     * @return
     */
    public BaseResponse<CenterCancelOrderRes> getCenterApplyRefund(CancelOrderReq req){
        BaseOrderRequest refundCheckRequest = new BaseOrderRequest();
        refundCheckRequest.setOrderId(req.getPartnerOrderId());
        refundCheckRequest.setSupplierOrderId(req.getOutOrderId());
        refundCheckRequest.setTraceId(req.getTraceId());
        UBRBaseResponse<UBRRefundCheckResponseCustom> refundCheckBaseResponse = ubrOrderService.refundCheck(refundCheckRequest);
        BigDecimal refundFee;
        BigDecimal channelRefundPrice;
        if(refundCheckBaseResponse != null && refundCheckBaseResponse.getCode() == 200
                && refundCheckBaseResponse.getData() != null && refundCheckBaseResponse.getData().getRefundAllow() != null
                && refundCheckBaseResponse.getData().getRefundAllow()){
            UBRRefundCheckResponseCustom response = refundCheckBaseResponse.getData();
            refundFee = response.getRefundFee();
            channelRefundPrice = response.getRefundPrice();
        } else {
            log.error("btg???????????????btg?????????????????????{}", refundCheckBaseResponse == null ? "null" : JSON.toJSONString(refundCheckBaseResponse));
            return BaseResponse.fail(CentralError.ERROR_SUPPLIER_APPLYREFUND_ORDER);
        }
        BaseOrderRequest orderRequest = new BaseOrderRequest();
        orderRequest.setTraceId(req.getTraceId());
        orderRequest.setOrderId(req.getPartnerOrderId());
        orderRequest.setSupplierOrderId(req.getOutOrderId());
        UBRBaseResponse<UBRTicketOrderResponse> baseResponse = ubrOrderService.refund(orderRequest);
        if(baseResponse != null && baseResponse.getCode() == 200){
            CenterCancelOrderRes centerCancelOrderRes = new CenterCancelOrderRes();
            centerCancelOrderRes.setOrderStatus(OrderStatus.APPLYING_FOR_REFUND.getCode());
            centerCancelOrderRes.setRefundFee(refundFee);
            centerCancelOrderRes.setChannelRefundPrice(channelRefundPrice);
            return BaseResponse.success(centerCancelOrderRes);
        }
        return BaseResponse.fail(CentralError.ERROR_SUPPLIER_APPLYREFUND_ORDER);
    }

    /**
     * ???????????????
     * @param req
     * @return
     */
    public  BaseResponse<CenterPayCheckRes> payCheck(PayOrderReq req){
        CenterPayCheckRes payCheckRes = new CenterPayCheckRes();
        payCheckRes.setResult(true);
        try {
            syncPriceV2(null, null, null, null, null);
            TripOrder tripOrder = tripOrderMapper.getOrderById(req.getPartnerOrderId());
            if(tripOrder == null || StringUtils.isBlank(tripOrder.getProductId()) || tripOrder.getQuantity() <= 0){
                log.info("????????????????????????????????????????????????????????????????????????orderId={}", req.getPartnerOrderId());
                return BaseResponse.success(payCheckRes);
            }
            ScenicSpotProductPriceMPO priceMPO = scenicSpotProductPriceDao.getPriceById(tripOrder.getProductId());
            if(tripOrder.getQuantity() > priceMPO.getStock()){
                log.info("??????????????????????????????????????????{}????????????{}", tripOrder.getQuantity(), priceMPO.getStock());
                return BaseResponse.withFail(CentralError.PRICE_CALC_STOCK_SHORT_ERROR);
            }
        } catch (Exception e) {
            log.error("????????????????????????????????????", e);
            return BaseResponse.success(payCheckRes);
        }
        return BaseResponse.success(payCheckRes);
    }

    /**
     * ????????????
     * @param req
     * @return
     */
    public  BaseResponse<CenterCancelOrderRes> getCenterCancelOrder(CancelOrderReq req){
        CenterCancelOrderRes centerCancelOrderRes = new CenterCancelOrderRes();
        centerCancelOrderRes.setOrderStatus(OrderStatus.CANCELLED.getCode());
        return BaseResponse.success(centerCancelOrderRes);
    }

    /**
     * ????????????
     * @param request
     * @return
     */
    public BaseResponse<CenterRefundCheckResult> refundCheck(CenterRefundCheckRequest request){
        CenterRefundCheckResult result = new CenterRefundCheckResult();
        BaseOrderRequest baseOrderRequest = new BaseOrderRequest();
        baseOrderRequest.setTraceId(request.getTraceId());
        baseOrderRequest.setSupplierOrderId(request.getSupplierOrderId());
        UBRBaseResponse<UBRRefundCheckResponseCustom> refundCheckBaseResponse = ubrOrderService.refundCheck(baseOrderRequest);
        if(refundCheckBaseResponse != null && refundCheckBaseResponse.getData() != null && refundCheckBaseResponse.getCode() == 200
                && refundCheckBaseResponse.getData().getRefundAllow() != null
                && refundCheckBaseResponse.getData().getRefundAllow()){
            result.setRefundFee(refundCheckBaseResponse.getData().getRefundFee());
            result.setAllowRefund(true);
            result.setChannelRefundPrice(refundCheckBaseResponse.getData().getRefundPrice());
            return BaseResponse.withSuccess(result);
        }
        return BaseResponse.withFail(CentralError.ERROR_NOT_ALLOW_REFUND);
    }

    /**
     * ??????????????????
     * @param productCode
     * @param supplierProductId
     * @param startDate
     * @param endDate
     * @param traceId
     */
    public void syncPriceV2(String productCode, String supplierProductId, String startDate, String endDate, String traceId){
        try {
            log.info("????????????????????????????????????");
            ubrProductService.getTicketList();
        } catch (Throwable e) {
            log.error("btg????????????????????????", e);
        }
    }

}
