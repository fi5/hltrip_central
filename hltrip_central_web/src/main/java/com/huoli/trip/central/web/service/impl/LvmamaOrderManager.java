package com.huoli.trip.central.web.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.huoli.trip.central.web.converter.OrderInfoTranser;
import com.huoli.trip.central.web.dao.PriceDao;
import com.huoli.trip.central.web.dao.ProductDao;
import com.huoli.trip.central.web.dao.ScenicSpotDao;
import com.huoli.trip.central.web.dao.ScenicSpotProductPriceDao;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.constant.ChannelConstant;
import com.huoli.trip.common.constant.*;
import com.huoli.trip.common.entity.ProductPO;
import com.huoli.trip.common.util.UploadUtil;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.ScenicSpotProductMPO;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.ScenicSpotProductPriceMPO;
import com.huoli.trip.common.vo.request.OrderOperReq;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.order.OrderDetailRep;
import com.huoli.trip.supplier.api.LvmamaOrderService;
import com.huoli.trip.supplier.self.lvmama.vo.LvOrderDetail;
import com.huoli.trip.central.web.converter.CreateOrderConverter;
import com.huoli.trip.common.vo.request.*;
import com.huoli.trip.common.vo.response.order.*;
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

import java.math.BigDecimal;
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

	@Autowired
	private ScenicSpotProductPriceDao scenicSpotProductPriceDao;

	@Autowired
	private ProductDao productDao;

	@Autowired
	private PriceDao priceDao;


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
			log.info("??????dfy???????????????????????????:"+ JSONObject.toJSONString(order));
			LvOrderDetail lmmOrderDetail = order.getData();
			//??????????????????,???????????????
			if( !order.isSuccess())
				return BaseResponse.fail(CentralError.ERROR_NO_ORDER);//?????????????????????????????????
			OrderDetailRep rep=new OrderDetailRep();
			rep.setOrderId(lmmOrderDetail.getOrderId());
			//?????????consumer?????????????????????
			rep.setOrderStatus(OrderInfoTranser.genCommonOrderStringStatus(lmmOrderDetail.getGjStatus(),5));
			rep.setVochers(genVouchers(lmmOrderDetail));
			return BaseResponse.success(rep);
		} catch (Exception e) {
			log.error("??????dfy????????????:"+JSONObject.toJSONString(req),e);
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
						String url = UploadUtil.decodeBase64ToImage(oneInfo.getQRcode(), detail.getOrderId()+".jpg");
						oneVoucher.setVocherUrl(url);
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
				log.error("genVouchers???", e);
			}
		}
		return null;
	}

	public BaseResponse<OrderDetailRep> getVochers(OrderOperReq req){
		try {
			LmmResendCodeRequest resendCodeRequest = new LmmResendCodeRequest();
			LmmResendCodeOrder lmmResendCodeOrder = new LmmResendCodeOrder(req.getOrderId(),req.getSupplierOrderId());
			resendCodeRequest.setOrder(lmmResendCodeOrder);
			resendCodeRequest.setTraceId(req.getTraceId());
			lvmamaOrderService.resendCode(resendCodeRequest);
			BaseOrderRequest baseOrderRequest = new BaseOrderRequest();
			baseOrderRequest.setOrderId(req.getOrderId());
			baseOrderRequest.setSupplierOrderId(req.getSupplierOrderId());
			baseOrderRequest.setTraceId(req.getTraceId());

			BaseResponse<LvOrderDetail> order = lvmamaOrderService.orderDetail(baseOrderRequest);
			if (order == null)
				return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
			log.info("getVochers??????dfy???????????????????????????:" + JSONObject.toJSONString(order));
			LvOrderDetail lmmOrderDetail = order.getData();
			//??????????????????,???????????????
			if (!order.isSuccess())
				return BaseResponse.fail(CentralError.ERROR_NO_ORDER);//?????????????????????????????????
			OrderDetailRep rep = new OrderDetailRep();
			rep.setOrderId(lmmOrderDetail.getOrderId());
			//?????????consumer?????????????????????
			rep.setOrderStatus(OrderInfoTranser.genCommonOrderStringStatus(lmmOrderDetail.getGjStatus(), 5));
			rep.setVochers(genVouchers(lmmOrderDetail));

			return BaseResponse.success(rep);


		} catch (Exception e) {
			log.error("DfygetVochers????????????:"+JSONObject.toJSONString(req),e);
			return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
		}
	}

	public BaseResponse<CenterBookCheck>  getCenterCheckInfos(BookCheckReq req){
		ValidateOrderRequest validateOrderRequest = new ValidateOrderRequest();
		validateOrderRequest.setTraceId(req.getTraceId());
		Long goodsId = 0l;
		Long productId = 0l;
		BigDecimal sellPrice;
		if(StringUtils.isNotBlank(req.getCategory())){
			//2021-05-31 ??????????????????
			ScenicSpotProductMPO scenicSpotProductMPO = scenicSpotDao.querySpotProductById(req.getProductId(), null);
			List<ScenicSpotProductPriceMPO> scenicSpotProductPriceMPOS = getPrice(req.getProductId(), req.getPackageId(), req.getBeginDate(), req.getEndDate());
			//2021-05-31 goodsid???productId???mongo???
			if(scenicSpotProductMPO != null){
				goodsId = Long.valueOf(scenicSpotProductMPO.getSupplierProductId());
				productId = Long.valueOf(scenicSpotProductMPO.getExtendParams().get("productId"));
			}
			if(!org.springframework.util.CollectionUtils.isEmpty(scenicSpotProductPriceMPOS)){
				sellPrice = scenicSpotProductPriceMPOS.get(0).getSettlementPrice();
				if(sellPrice != null){
					req.setSellPrice(sellPrice.toPlainString());
				}
			}
		} else {
			ProductPO productPO = productDao.getTripProductByCode(req.getProductId());
			if(productPO != null){
				goodsId = Long.valueOf(productPO.getSupplierProductId());
				productId = Long.valueOf(productPO.getExtendParams().get("productId"));
			}
		}
		createOrderConverter.convertLvmamaBookOrderRequest(validateOrderRequest,req, goodsId, productId);

		LmmBaseResponse checkInfos = lvmamaOrderService.getCheckInfos(validateOrderRequest);
		if(!"1000".equals(checkInfos.getState().getCode())){
			return BaseResponse.fail(CentralError.ERROR_SUPPLIER_BOOK_CHECK_ORDER);
		}
		return BaseResponse.withSuccess();
	}

	private List<ScenicSpotProductPriceMPO> getPrice(String productId, String packageId, String startDate, String endDate){
		String ruleId = null;
		String ticketKind = null;
		// ???????????????????????????packageId????????????????????????????????????productid?????????
		if(org.apache.commons.lang3.StringUtils.isNotBlank(packageId)){
			ScenicSpotProductPriceMPO priceMPO = scenicSpotProductPriceDao.getPriceById(packageId);
			if(priceMPO != null){
				ruleId = priceMPO.getScenicSpotRuleId();
				ticketKind = priceMPO.getTicketKind();
			}
		}
		return scenicSpotDao.queryPrice(productId, startDate, endDate, ruleId, ticketKind);
	}

	public BaseResponse<CenterCreateOrderRes> getCenterCreateOrder(CreateOrderReq req){
		CreateOrderRequest request = new CreateOrderRequest();
		request.setTraceId(req.getTraceId());
		//2021-05-31 ??????????????????
		Long goodsId = 0l;
		Long productId = 0l;
		BigDecimal sellPrice;
		if(StringUtils.isNotBlank(req.getCategory())){
			//2021-05-31 ??????????????????
			ScenicSpotProductMPO scenicSpotProductMPO = scenicSpotDao.querySpotProductById(req.getProductId(), null);
			List<ScenicSpotProductPriceMPO> scenicSpotProductPriceMPOS = getPrice(req.getProductId(), req.getPackageId(), req.getBeginDate(), req.getEndDate());
			//2021-05-31 goodsid???productId???mongo???
			if(scenicSpotProductMPO != null){
				goodsId = Long.valueOf(scenicSpotProductMPO.getSupplierProductId());
				productId = Long.valueOf(scenicSpotProductMPO.getExtendParams().get("productId"));
			}
			if(!org.springframework.util.CollectionUtils.isEmpty(scenicSpotProductPriceMPOS)){
				sellPrice = scenicSpotProductPriceMPOS.get(0).getSettlementPrice();
				if(sellPrice != null){
					req.setSellPrice(sellPrice.toPlainString());
				}
			}
		} else {
			ProductPO productPO = productDao.getTripProductByCode(req.getProductId());
			if(productPO != null){
				goodsId = Long.valueOf(productPO.getSupplierProductId());
				productId = Long.valueOf(productPO.getExtendParams().get("productId"));
			}
		}
		createOrderConverter.convertLvmamaCreateOrderRequest(request,req, goodsId, productId);
		OrderResponse response = lvmamaOrderService.createOrder(request);
		if(response != null && "1000".equals(response.getState().getCode())){
			CenterCreateOrderRes centerCreateOrderRes = new CenterCreateOrderRes();
			centerCreateOrderRes.setOrderId(response.getOrder().getOrderId());
			//????????????????????????
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
			//????????????????????????
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
			CenterCancelOrderRes centerCancelOrderRes = new CenterCancelOrderRes();
			centerCancelOrderRes.setOrderStatus(OrderStatus.APPLYING_FOR_REFUND.getCode());
			return BaseResponse.withSuccess(centerCancelOrderRes);
		}
		return BaseResponse.fail(CentralError.ERROR_SUPPLIER_CANCEL_ORDER);
	}

}
