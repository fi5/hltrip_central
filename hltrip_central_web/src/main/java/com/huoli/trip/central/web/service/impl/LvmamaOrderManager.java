package com.huoli.trip.central.web.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.huoli.trip.central.web.converter.OrderInfoTranser;
import com.huoli.trip.central.web.dao.ScenicSpotDao;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.constant.ChannelConstant;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.ScenicSpotProductMPO;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.ScenicSpotProductPriceMPO;
import com.huoli.trip.common.vo.request.OrderOperReq;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.order.OrderDetailRep;
import com.huoli.trip.supplier.api.LvmamaOrderService;
import com.huoli.trip.supplier.self.difengyun.vo.DfyToursOrderDetail;
import com.huoli.trip.supplier.self.lvmama.vo.LvOrderDetail;
import com.huoli.trip.central.web.converter.CreateOrderConverter;
import com.huoli.trip.central.web.converter.OrderInfoTranser;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.constant.ChannelConstant;
import com.huoli.trip.common.vo.request.*;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.order.*;
import com.huoli.trip.supplier.api.DfyOrderService;
import com.huoli.trip.supplier.api.LvmamaOrderService;
import com.huoli.trip.supplier.self.difengyun.vo.DfyToursOrderDetail;
import com.huoli.trip.supplier.self.lvmama.vo.OrderInfo;
import com.huoli.trip.supplier.self.lvmama.vo.OrderPaymentInfo;
import com.huoli.trip.supplier.self.lvmama.vo.request.*;
import com.huoli.trip.supplier.self.lvmama.vo.response.LmmBaseResponse;
import com.huoli.trip.supplier.self.lvmama.vo.response.OrderResponse;
import com.huoli.trip.supplier.self.yaochufa.vo.BaseOrderRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
@Slf4j
public class LvmamaOrderManager extends OrderManager {


	@Reference(timeout = 10000,group = "hltrip", check = false)
	private LvmamaOrderService lvmamaOrderService;

	@Autowired
	private CreateOrderConverter createOrderConverter;

	@Autowired
	private ScenicSpotDao scenicSpotDao;


	public final static String CHANNEL= ChannelConstant.SUPPLIER_TYPE_LMM;
	public String getChannel(){
		return CHANNEL;
	}
	@Override
	public String test() {
		return "lvmama";
	}


	public BaseResponse<OrderDetailRep> getOrderDetail(OrderOperReq req){
		BaseOrderRequest baseOrderRequest = new BaseOrderRequest();
		baseOrderRequest.setOrderId(req.getOrderId());
		baseOrderRequest.setTraceId(req.getTraceId());
		baseOrderRequest.setSupplierOrderId(req.getSupplierOrderId());
		BaseResponse<LvOrderDetail> order = lvmamaOrderService.orderDetail(baseOrderRequest);
		if(order==null)
			return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
		try {
			log.info("中台dfy跟团拿到的订单数据:"+ JSONObject.toJSONString(order));
			LvOrderDetail lmmOrderDetail = order.getData();
			//如果数据为空,直接返回错
			if( !order.isSuccess())
				return BaseResponse.fail(CentralError.ERROR_NO_ORDER);//异常消息以供应商返回的
			OrderDetailRep rep=new OrderDetailRep();
			rep.setOrderId(lmmOrderDetail.getOrderId());
			//转换成consumer统一的订单状态
			rep.setOrderStatus(OrderInfoTranser.genCommonOrderStringStatus(lmmOrderDetail.getGjStatus(),5));
			rep.setVochers(genVouchers(lmmOrderDetail));
			return BaseResponse.success(rep);
		} catch (Exception e) {
			log.error("中台dfy跟团报错:"+JSONObject.toJSONString(req),e);
			return BaseResponse.fail(9999,order.getMessage(),null);
		}

	}

	private List<OrderDetailRep.Voucher> genVouchers(LvOrderDetail detail) {
		if (CollectionUtils.isNotEmpty(detail.getCredentials())) {
			List<OrderDetailRep.Voucher> vochers = new ArrayList<>();
			try {
				for (LvOrderDetail.Credential oneInfo : detail.getCredentials()) {

					if (StringUtils.isNotBlank(oneInfo.getQRcode())) {
						OrderDetailRep.Voucher oneVoucher = new OrderDetailRep.Voucher();
						oneVoucher.setVocherUrl(oneInfo.getQRcode());
						oneVoucher.setType(2);
						vochers.add(oneVoucher);
					}
					if (StringUtils.isNotBlank(oneInfo.getVoucherUrl())) {
						OrderDetailRep.Voucher oneVoucher = new OrderDetailRep.Voucher();
						oneVoucher.setVocherUrl(oneInfo.getVoucherUrl());
						oneVoucher.setType(2);
						vochers.add(oneVoucher);
					}
					if (StringUtils.isNotBlank(oneInfo.getSerialCode())) {
						OrderDetailRep.Voucher oneVoucher = new OrderDetailRep.Voucher();
						oneVoucher.setVocherNo(oneInfo.getSerialCode());
						oneVoucher.setType(1);
						vochers.add(oneVoucher);
					}
					if (StringUtils.isNotBlank(oneInfo.getAdditional())) {
						OrderDetailRep.Voucher oneVoucher = new OrderDetailRep.Voucher();
						oneVoucher.setVocherNo(oneInfo.getAdditional());
						oneVoucher.setType(1);
						vochers.add(oneVoucher);
					}

				}
				return vochers;

			} catch (Exception e) {
				log.error("genVouchers错", e);
			}
		}
		return null;
	}

	public BaseResponse<OrderDetailRep> getVochers(OrderOperReq req){

		try {
			BaseOrderRequest baseOrderRequest = new BaseOrderRequest();
			baseOrderRequest.setOrderId(req.getOrderId());
			baseOrderRequest.setSupplierOrderId(req.getSupplierOrderId());
			baseOrderRequest.setTraceId(req.getTraceId());

			BaseResponse<LvOrderDetail> order = lvmamaOrderService.orderDetail(baseOrderRequest);
			if (order == null)
				return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
			log.info("getVochers中台dfy跟团拿到的订单数据:" + JSONObject.toJSONString(order));
			LvOrderDetail lmmOrderDetail = order.getData();
			//如果数据为空,直接返回错
			if (!order.isSuccess())
				return BaseResponse.fail(CentralError.ERROR_NO_ORDER);//异常消息以供应商返回的
			OrderDetailRep rep = new OrderDetailRep();
			rep.setOrderId(lmmOrderDetail.getOrderId());
			//转换成consumer统一的订单状态
			rep.setOrderStatus(OrderInfoTranser.genCommonOrderStringStatus(lmmOrderDetail.getGjStatus(), 5));
			rep.setVochers(genVouchers(lmmOrderDetail));

			return BaseResponse.success(rep);


		} catch (Exception e) {
			log.error("DfygetVochers跟团报错:"+JSONObject.toJSONString(req),e);
			return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
		}
	}

	public BaseResponse<CenterBookCheck>  getCenterCheckInfos(BookCheckReq req){
		ValidateOrderRequest validateOrderRequest = new ValidateOrderRequest();
		validateOrderRequest.setTraceId(req.getTraceId());
		//2021-05-31 获取产品信息
		ScenicSpotProductMPO scenicSpotProductMPO = scenicSpotDao.querySpotProductById(req.getProductId());
		List<ScenicSpotProductPriceMPO> scenicSpotProductPriceMPOS = scenicSpotDao.queryPriceByProductIdAndDate(req.getProductId(), req.getBeginDate(), req.getBeginDate());
		createOrderConverter.convertLvmamaBookOrderRequest(validateOrderRequest,req, scenicSpotProductMPO, scenicSpotProductPriceMPOS);

		LmmBaseResponse checkInfos = lvmamaOrderService.getCheckInfos(validateOrderRequest);
		if(!"1000".equals(checkInfos.getState().getCode())){
			return BaseResponse.fail(CentralError.ERROR_SUPPLIER_BOOK_CHECK_ORDER);
		}
		return BaseResponse.withSuccess();
	}
	public BaseResponse<CenterCreateOrderRes> getCenterCreateOrder(CreateOrderReq req){
		CreateOrderRequest request = new CreateOrderRequest();
		request.setTraceId(req.getTraceId());
		//2021-05-31 获取产品信息
		ScenicSpotProductMPO scenicSpotProductMPO = scenicSpotDao.querySpotProductById(req.getProductId());
		List<ScenicSpotProductPriceMPO> scenicSpotProductPriceMPOS = scenicSpotDao.queryPriceByProductIdAndDate(req.getProductId(), req.getBeginDate(), req.getBeginDate());
		createOrderConverter.convertLvmamaCreateOrderRequest(request,req, scenicSpotProductMPO, scenicSpotProductPriceMPOS);
		OrderResponse response = lvmamaOrderService.createOrder(request);
		if(response != null && "1000".equals(response.getState().getCode())){
			CenterCreateOrderRes centerCreateOrderRes = new CenterCreateOrderRes();
			centerCreateOrderRes.setOrderId(response.getOrder().getOrderId());
			//订单状态需要转换
			final String paymentStatus = response.getOrder().getStatus();
			final int i = createOrderConverter.convertLvmamaOrderStatus(paymentStatus);
			//final int i = OrderInfoTranser.genCommonOrderStringStatus(paymentStatus, 5);
			centerCreateOrderRes.setOrderStatus(i);
			return BaseResponse.withSuccess(centerCreateOrderRes);
		}
		return BaseResponse.fail(CentralError.ERROR_ORDER_CREATE_SUPPLIER);
	}

	public BaseResponse<CenterPayOrderRes> getCenterPayOrder(PayOrderReq req){
		OrderPaymentRequest orderPaymentRequest = new OrderPaymentRequest();
		OrderPaymentInfo orderPaymentInfo = new OrderPaymentInfo();
		orderPaymentRequest.setOrder(orderPaymentInfo);
		orderPaymentInfo.setOrderId(req.getChannelOrderId());
		orderPaymentInfo.setPartnerOrderNo(req.getPartnerOrderId());
		orderPaymentInfo.setSerialNum(req.getPaySerialNumber());
		final OrderResponse orderResponse = lvmamaOrderService.payOrder(orderPaymentRequest);
		if(orderResponse != null && "1000".equals(orderResponse.getState().getCode())){
			CenterPayOrderRes centerCreateOrderRes = new CenterPayOrderRes();
			centerCreateOrderRes.setChannelOrderId(req.getChannelOrderId());
			centerCreateOrderRes.setLocalOrderId(req.getPartnerOrderId());
			//订单状态需要转换
			final String paymentStatus = orderResponse.getOrder().getPaymentStatus();
			final int i = createOrderConverter.convertLvmamaOrderStatus(paymentStatus);
			//final int i = OrderInfoTranser.genCommonOrderStringStatus(paymentStatus, 5);
			centerCreateOrderRes.setOrderStatus(i);
			return BaseResponse.withSuccess(centerCreateOrderRes);
		}
		return BaseResponse.fail(CentralError.ERROR_ORDER_PAY);
	}

	public  BaseResponse<CenterCancelOrderRes> getCenterCancelOrder(CancelOrderReq req){
		OrderUnpaidCancelRequest request = new OrderUnpaidCancelRequest(req.getPartnerOrderId(),req.getOutOrderId());
		final OrderResponse orderResponse = lvmamaOrderService.cancelOrder(request);
		if(orderResponse != null && "1000".equals(orderResponse.getState().getCode())){

			return BaseResponse.withSuccess();
		}
		return BaseResponse.fail(CentralError.ERROR_SUPPLIER_CANCEL_ORDER);
	}

	public  BaseResponse<CenterPayCheckRes> payCheck(PayOrderReq req){
		CenterPayCheckRes  payCheckRes = new CenterPayCheckRes();
		payCheckRes.setResult(true);
		return BaseResponse.withSuccess(payCheckRes);
	}

	public BaseResponse<CenterCancelOrderRes> getCenterApplyRefund(CancelOrderReq req){
		OrderCancelRequest request = new OrderCancelRequest(req.getPartnerOrderId(),req.getOutOrderId());
		LmmBaseResponse baseResponse = lvmamaOrderService.refundTicket(request);
		if(baseResponse != null && "1000".equals(baseResponse.getState().getCode())){
			return BaseResponse.withSuccess();
		}
		return BaseResponse.fail(CentralError.ERROR_SUPPLIER_CANCEL_ORDER);
	}

}
