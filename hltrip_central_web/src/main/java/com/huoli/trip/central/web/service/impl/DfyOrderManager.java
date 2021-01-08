package com.huoli.trip.central.web.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.huoli.trip.central.api.ProductService;
import com.huoli.trip.central.web.converter.OrderInfoTranser;
import com.huoli.trip.central.web.converter.SupplierErrorMsgTransfer;
import com.huoli.trip.central.web.util.TraceIdUtils;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.constant.ChannelConstant;
import com.huoli.trip.common.constant.OrderStatus;
import com.huoli.trip.common.exception.HlCentralException;
import com.huoli.trip.common.util.DateTimeUtil;
import com.huoli.trip.common.util.ListUtils;
import com.huoli.trip.common.vo.request.*;
import com.huoli.trip.common.vo.request.central.PriceCalcRequest;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.central.PriceCalcResult;
import com.huoli.trip.common.vo.response.order.*;
import com.huoli.trip.supplier.api.DfyOrderService;
import com.huoli.trip.supplier.api.YcfOrderService;
import com.huoli.trip.supplier.api.YcfSyncService;
import com.huoli.trip.supplier.self.difengyun.DfyOrderDetail;
import com.huoli.trip.supplier.self.difengyun.constant.DfyCertificateType;
import com.huoli.trip.supplier.self.difengyun.vo.Contact;
import com.huoli.trip.supplier.self.difengyun.vo.DfyBookSaleInfo;
import com.huoli.trip.supplier.self.difengyun.vo.Tourist;
import com.huoli.trip.supplier.self.difengyun.vo.request.*;
import com.huoli.trip.supplier.self.difengyun.vo.response.DfyBaseResult;
import com.huoli.trip.supplier.self.difengyun.vo.response.DfyBookCheckResponse;
import com.huoli.trip.supplier.self.difengyun.vo.response.DfyCreateOrderResponse;
import com.huoli.trip.supplier.self.difengyun.vo.response.DfyRefundTicketResponse;
import com.huoli.trip.supplier.self.difengyun.vo.response.*;
import com.huoli.trip.supplier.self.hllx.vo.*;
import com.huoli.trip.supplier.self.yaochufa.vo.BaseOrderRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
//
//import java.util.ArrayList;
//import java.util.List;

/**
 * 描述：desc<br>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：顾刘川<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/1<br>
 */
@Component
@Slf4j
public class DfyOrderManager extends OrderManager {


    @Reference(timeout = 10000,group = "hltrip", check = false)
    private DfyOrderService dfyOrderService;

    @Autowired
    private ProductService productService;




    public final static String CHANNEL= ChannelConstant.SUPPLIER_TYPE_DFY;
    public String getChannel(){
        return CHANNEL;
    }
    @Override
    public String test() {
        System.out.println("dfy");
        return "dfy";
    }


    public BaseResponse<OrderDetailRep> getOrderDetail(OrderOperReq req){
        BaseOrderRequest baseOrderRequest = new BaseOrderRequest();
        baseOrderRequest.setOrderId(req.getOrderId());
        baseOrderRequest.setTraceId(req.getTraceId());
        baseOrderRequest.setSupplierOrderId(req.getSupplierOrderId());
        final BaseResponse<DfyOrderDetail> order = dfyOrderService.orderDetail(baseOrderRequest);
        if(order==null)
            return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
        try {
            log.info("dfy拿到的订单数据:"+ JSONObject.toJSONString(order));
            DfyOrderDetail dfyOrderDetail = order.getData();
            //如果数据为空,直接返回错
            if( !order.isSuccess())
                return BaseResponse.fail(CentralError.ERROR_NO_ORDER);//异常消息以供应商返回的
            OrderDetailRep rep=new OrderDetailRep();
            rep.setOrderId(dfyOrderDetail.getOrderId());
            //转换成consumer统一的订单状态
            rep.setOrderStatus(OrderInfoTranser.genCommonOrderStringStatus(dfyOrderDetail.getOrderStatus(),3));
//            rep.setVochers(JSONObject.parseArray(JSONObject.toJSONString(data.getVochers()), OrderDetailRep.Voucher.class));
            return BaseResponse.success(rep);
        } catch (Exception e) {
            log.error("DfygetOrderDetail报错:"+JSONObject.toJSONString(req),e);
            return BaseResponse.fail(9999,order.getMessage(),null);
        }

    }

    public BaseResponse<OrderDetailRep> getVochers(OrderOperReq req){

        try {
            BaseOrderRequest baseOrderRequest = new BaseOrderRequest();
            baseOrderRequest.setOrderId(req.getOrderId());
            baseOrderRequest.setSupplierOrderId(req.getSupplierOrderId());
            baseOrderRequest.setTraceId(req.getTraceId());
//            final BaseResponse<OrderDetailRep> vochers = dfyOrderService.getVochers(baseOrderRequest);
//            if(null==vochers)
//                return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
//            log.info("dfy拿到的数据:"+JSONObject.toJSONString(vochers));
//            OrderDetailRep detailData = vochers.getData();
//            if(!vochers.isSuccess())
//                return BaseResponse.fail(OrderInfoTranser.findCentralError(vochers.getMessage()));//异常消息以供应商返回的
//            return BaseResponse.success(detailData);


            BaseResponse<DfyOrderDetail> order = dfyOrderService.orderDetail(baseOrderRequest);
            if (order == null)
                return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
            log.info("dfy拿到的订单数据:" + JSONObject.toJSONString(order));
            DfyOrderDetail dfyOrderDetail = order.getData();
            //如果数据为空,直接返回错
            if (!order.isSuccess())
                return BaseResponse.fail(CentralError.ERROR_NO_ORDER);//异常消息以供应商返回的
            OrderDetailRep rep = new OrderDetailRep();
            rep.setOrderId(dfyOrderDetail.getOrderId());
            //转换成consumer统一的订单状态
            rep.setOrderStatus(OrderInfoTranser.genCommonOrderStringStatus(dfyOrderDetail.getOrderStatus(), 3));


            if(null!=dfyOrderDetail.getOrderInfo().getEnterCertificate()&& CollectionUtils.isNotEmpty(dfyOrderDetail.getOrderInfo().getEnterCertificate().getEnterCertificateTypeInfo())){
                List<OrderDetailRep.Voucher> vochers = new ArrayList<>();
                for(DfyOrderDetail.EnterCertificateTypeInfo typeInfo:dfyOrderDetail.getOrderInfo().getEnterCertificate().getEnterCertificateTypeInfo()){
                    for(DfyOrderDetail.TicketCertInfo oneInfo:typeInfo.getTicketCertInfos()){

                        switch (oneInfo.getCertType()){//凭证类型   1.纯文本  2.二维码 3.PDF
                        	case 1:
                                for(String entry:oneInfo.getFileUrls()){
                                    OrderDetailRep.Voucher oneVoucher=new OrderDetailRep.Voucher();
                                    oneVoucher.setVocherNo(entry);
                                    oneVoucher.setType(1);
                                    vochers.add(oneVoucher);
                                }
                        	break;

                        	case 2:
                                for(String entry:oneInfo.getFileUrls()){
                                    OrderDetailRep.Voucher oneVoucher=new OrderDetailRep.Voucher();
                                    oneVoucher.setVocherUrl(entry);
                                    oneVoucher.setType(2);
                                    vochers.add(oneVoucher);
                                }
                        		break;
                            case 3:
                                for(String entry:oneInfo.getFileUrls()){
                                    OrderDetailRep.Voucher oneVoucher=new OrderDetailRep.Voucher();
                                    oneVoucher.setVocherUrl(entry);
                                    oneVoucher.setType(3);
                                    vochers.add(oneVoucher);
                                }
                                break;
                        }
                    }
                }
                rep.setVochers(vochers);
            }


            return BaseResponse.success(rep);


        } catch (Exception e) {
            log.error("DfygetVochers报错:"+JSONObject.toJSONString(req),e);
            return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
        }

    }


    /**
     * 可预订检查
     * @param req
     * @return
     */
    public BaseResponse<CenterBookCheck> getCenterCheckInfos(BookCheckReq req){
        log.info("进入可预订检查 dfy");
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
        DfyBookCheckRequest req1 = new DfyBookCheckRequest();
        //转供应商productId
        //ycfBookCheckReq.setProductId(CentralUtils.getSupplierId(req.getProductId()));
        req1.setBeginDate(begin);
        req1.setEndDate(end);
        req1.setProductId(req.getProductId());
        String traceId = req.getTraceId();
        if(org.apache.commons.lang3.StringUtils.isEmpty(traceId)){
            traceId = TraceIdUtils.getTraceId();
        }
        req1.setTraceId(traceId);
        try {
            //供应商输出
            DfyBaseResult<DfyBookCheckResponse> checkInfos = dfyOrderService.getCheckInfos(req1);
            if(checkInfos!=null && checkInfos.getStatusCode()==200){
                DfyBookCheckResponse dfyBookCheckResponse = checkInfos.getData();
                if(dfyBookCheckResponse == null){
                    return SupplierErrorMsgTransfer.buildMsg(checkInfos.getMsg());//异常消息以供应商返回的
                }else{
//                    CenterBookCheck  bookCheck = new CenterBookCheck();
                    List<DfyBookSaleInfo> saleInfos = dfyBookCheckResponse.getSaleInfos();
                    if(ListUtils.isEmpty(saleInfos)){
                        return BaseResponse.fail(CentralError.NO_STOCK_ERROR);
                    }
                    DfyBookSaleInfo dfyBookSaleInfo = saleInfos.get(0);
                    int stocks = dfyBookSaleInfo.getTotalStock();
                    if(req.getCount() > stocks){
                        return BaseResponse.withFail(CentralError.NOTENOUGH_STOCK_ERROR, null);
                    }
//
                }
            }else{
                return BaseResponse.fail(CentralError.ERROR_SUPPLIER_BOOK_CHECK_ORDER);
            }
        }catch (HlCentralException e){
            return BaseResponse.fail(CentralError.ERROR_SUPPLIER_BOOK_CHECK_ORDER);
        }
        CenterBookCheck  bookCheck = new CenterBookCheck();
        PriceCalcRequest calcRequest = new PriceCalcRequest();
        calcRequest.setStartDate(DateTimeUtil.parseDate(begin));
        calcRequest.setEndDate(DateTimeUtil.parseDate(end));
        calcRequest.setProductCode(req.getProductId());
        calcRequest.setQuantity(req.getCount());
        PriceCalcResult priceCalcResult = null;
        calcRequest.setTraceId(traceId);
        try{
            BaseResponse<PriceCalcResult> priceCalcResultBaseResponse = productService.calcTotalPrice(calcRequest);
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


    /**
     * 创建订单
     * @param req
     * @return
     */
    public BaseResponse<CenterCreateOrderRes> getCenterCreateOrder(CreateOrderReq req){
        DfyCreateOrderRequest dfyCreateOrderRequest = new DfyCreateOrderRequest();
        dfyCreateOrderRequest.setStartTime(req.getBeginDate());
        dfyCreateOrderRequest.setProductId(req.getProductId().split("_")[1]);
        dfyCreateOrderRequest.setBookNumber(req.getQunatity());
        Contact contact  = new Contact();
        contact.setContactTel(req.getMobile());
        contact.setContactName(req.getEmail());
        contact.setContactName(req.getCname());
        int psptc = req.getCredentialType();
        int dfypsc = changecredentialType(psptc);
        String psptId = req.getCredential();
        if(StringUtils.isNotEmpty(psptId)) {
            DfyCertificateType certificateByCode = DfyCertificateType.getCertificateByCode(dfypsc);
            if (certificateByCode != null) {
                contact.setPsptType(certificateByCode.getCode());
                contact.setPsptId(psptId);
            }
        }
        //contact.setPsptId();
        //contact.setPsptType();
        dfyCreateOrderRequest.setContact(contact);

        //出行人
        List<CreateOrderReq.BookGuest> guests = req.getGuests();
        if(ListUtils.isNotEmpty(guests)) {
            List<Tourist> touristList = new ArrayList<>();
            for (CreateOrderReq.BookGuest guest: guests) {
                Tourist tourist = new Tourist();
                tourist.setName(guest.getCname());
                tourist.setEmail(guest.getEmail());
                tourist.setTel(guest.getMobile());
                int psptcode = guest.getCredentialType();
                int dfypscode = changecredentialType(psptcode);
                DfyCertificateType certificateByCode = DfyCertificateType.getCertificateByCode(dfypscode);
                if (certificateByCode != null) {
                    tourist.setPsptType(certificateByCode.getCode());
                    tourist.setPsptId(guest.getCredential());
                }
                touristList.add(tourist);
            }
            dfyCreateOrderRequest.setTouristList(touristList);
        }

        String traceId = req.getTraceId();
        if(org.apache.commons.lang3.StringUtils.isEmpty(traceId)){
            traceId = TraceIdUtils.getTraceId();
        }
        dfyCreateOrderRequest.setTraceId(traceId);
        DfyBaseResult<DfyCreateOrderResponse> order = dfyOrderService.createOrder(dfyCreateOrderRequest);
        if(order != null && order.getStatusCode() == 200 && order.getData() != null) {
            CenterCreateOrderRes createOrderRes = new CenterCreateOrderRes();
            //订单号怎么处理
            createOrderRes.setOrderId(order.getData().getOrderId());
            createOrderRes.setOrderStatus(OrderStatus.TO_BE_PAID.getCode());
            return BaseResponse.success(createOrderRes);
        }
        return BaseResponse.fail(CentralError.ERROR_ORDER_TRIP_ORDER_ERROR);
    }


    /**
     * 支付订单
     * @param req
     * @return
     */
    public BaseResponse<CenterPayOrderRes> getCenterPayOrder(PayOrderReq req){
        DfyPayOrderRequest request = new DfyPayOrderRequest();
        request.setTraceId(req.getTraceId());
        request.setChannelCode(req.getChannelCode());
        request.setChannelOrderId(req.getChannelOrderId());
        request.setPrice(String.valueOf(req.getPrice().intValue()));
        DfyBaseResult dfyBaseResult = dfyOrderService.payOrder(request);
        if(dfyBaseResult != null && dfyBaseResult.isSuccess()){
            CenterPayOrderRes payOrderRes = new CenterPayOrderRes();
            payOrderRes.setChannelOrderId(req.getChannelOrderId());
            payOrderRes.setLocalOrderId(req.getPartnerOrderId());
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
        DfyCancelOrderRequest dfyCancelOrderRequest = new DfyCancelOrderRequest();
        dfyCancelOrderRequest.setOrderId(req.getOutOrderId());
        String remark = req.getRemark();
        if(StringUtils.isEmpty(remark)) {
            remark = "行程变更";
            dfyCancelOrderRequest.setRemark(remark);
        }
        String traceId = req.getTraceId();
        if(org.apache.commons.lang3.StringUtils.isEmpty(traceId)){
            traceId = TraceIdUtils.getTraceId();
        }
        dfyCancelOrderRequest.setTraceId(traceId);
        DfyBaseResult dfyBaseResult = dfyOrderService.cancelOrder(dfyCancelOrderRequest);
        if(dfyBaseResult != null && dfyBaseResult.isSuccess() && dfyBaseResult.getData() != null){
            CenterCancelOrderRes centerCancelOrderRes = new CenterCancelOrderRes();
            /*centerCancelOrderRes.setOrderStatus(dfyBaseResult.getData().getOrderStatus());*/
            DfyOrderStatusRequest dfyOrderStatusRequest = new DfyOrderStatusRequest();
            dfyOrderStatusRequest.setOrderId(req.getOutOrderId());
            DfyBaseResult<DfyOrderStatusResponse> dfyOrderStatusResponseDfyBaseResult = dfyOrderService.orderStatus(dfyOrderStatusRequest);
            if(dfyOrderStatusResponseDfyBaseResult != null && dfyOrderStatusResponseDfyBaseResult.isSuccess() && dfyOrderStatusResponseDfyBaseResult.getData() != null){
                String orderStatus = dfyOrderStatusResponseDfyBaseResult.getData().getOrderStatus();
                int i = OrderInfoTranser.genCommonOrderStringStatus(orderStatus, 3);
                centerCancelOrderRes.setOrderStatus(i);
            }else{
                centerCancelOrderRes.setOrderStatus(OrderStatus.APPLYING_FOR_REFUND.getCode());
            }
            return BaseResponse.success(centerCancelOrderRes);
        }
        return BaseResponse.fail(CentralError.ERROR_SUPPLIER_CANCEL_ORDER);
    }


    /**
     * 支付前校验
     * @param req
     * @return
     */
    public  BaseResponse<CenterPayCheckRes> payCheck(PayOrderReq req){
        CenterPayCheckRes  payCheckRes = new CenterPayCheckRes();
        BaseOrderRequest baseOrderRequest = new BaseOrderRequest();
        baseOrderRequest.setOrderId(req.getPartnerOrderId());
        baseOrderRequest.setTraceId(req.getTraceId());
        baseOrderRequest.setSupplierOrderId(req.getChannelOrderId());
        BaseResponse<DfyOrderDetail> dfyOrderDetailBaseResponse = dfyOrderService.orderDetail(baseOrderRequest);
        if(dfyOrderDetailBaseResponse.isSuccess() && dfyOrderDetailBaseResponse.getData() != null){
            DfyOrderDetail.OrderInfo orderInfo = dfyOrderDetailBaseResponse.getData().getOrderInfo();
            String status = dfyOrderDetailBaseResponse.getData().getOrderStatus();
            String canPay = orderInfo.getCanPay();
            if("待付款".equals(status) && "1".equals(canPay)){
                payCheckRes.setResult(true);
                //payCheckRes.setCode(String.valueOf(CentralError.SUPPLIER_PAY_CHECK_SUCCESS.getCode()));
                return BaseResponse.success(payCheckRes);
            }else{
                //稍后重试
                payCheckRes.setResult(false);
                return BaseResponse.fail(CentralError.SUPPLIER_PAY_CHECK_WAITING);
            }
        }else{
            payCheckRes.setResult(false);
            return BaseResponse.fail(CentralError.ERROR_ORDER_PAY_BEFORE);
        }
    }


    /**
     * 申请退款
     * @param req
     * @return
     */
    public BaseResponse<CenterCancelOrderRes> getCenterApplyRefund(CancelOrderReq req){
        DfyRefundTicketRequest dfyRefundTicketRequest = new DfyRefundTicketRequest();
        dfyRefundTicketRequest.setTraceId(req.getTraceId());
        dfyRefundTicketRequest.setOrderId(req.getOutOrderId());
        dfyRefundTicketRequest.setCauseType("5");
        String remark = req.getRemark();
        if(StringUtils.isEmpty(remark)){
            remark = "发生意外,无法出行！";
        }
        dfyRefundTicketRequest.setCauseContent(remark);
        DfyBaseResult<DfyRefundTicketResponse> dfyRefundTicketResponseDfyBaseResult = dfyOrderService.rufundTicket(dfyRefundTicketRequest);
        if(dfyRefundTicketResponseDfyBaseResult != null && dfyRefundTicketResponseDfyBaseResult.isSuccess()){
            CenterCancelOrderRes centerCancelOrderRes = new CenterCancelOrderRes();
            centerCancelOrderRes.setOrderStatus(OrderStatus.APPLYING_FOR_REFUND.getCode());
            return BaseResponse.success(centerCancelOrderRes);
        }
        return BaseResponse.fail(CentralError.ERROR_SUPPLIER_APPLYREFUND_ORDER);
    }

    private Integer changecredentialType(int guestsCredentialType){
        Integer result = null;
        switch(guestsCredentialType){
            case 0 :
                result = 1;
                break; //可选
            case 1 :
                result= 2;
                break;
            case 2:
                result = 4;
            case 3:
                break;
            case 4 :
                break; //可选
            case 5 :
                result = 7;
                break;
            case 6:
            case 7:
                result = 3;
                break;
            default : //可选
                //语句
        }
        return result;
    }


}
