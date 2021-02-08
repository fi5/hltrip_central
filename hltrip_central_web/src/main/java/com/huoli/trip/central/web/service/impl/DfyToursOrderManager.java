package com.huoli.trip.central.web.service.impl;

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
import com.huoli.trip.supplier.self.difengyun.DfyOrderDetail;
import com.huoli.trip.supplier.self.difengyun.constant.DfyCertificateType;
import com.huoli.trip.supplier.self.difengyun.vo.Contact;
import com.huoli.trip.supplier.self.difengyun.vo.DfyBookSaleInfo;
import com.huoli.trip.supplier.self.difengyun.vo.DfyToursOrderDetail;
import com.huoli.trip.supplier.self.difengyun.vo.Tourist;
import com.huoli.trip.supplier.self.difengyun.vo.request.*;
import com.huoli.trip.supplier.self.difengyun.vo.response.*;
import com.huoli.trip.supplier.self.yaochufa.vo.BaseOrderRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author :zhouwenbin
 * @time   :2021/1/28
 * @comment:
 **/
@Component
@Slf4j
public class DfyToursOrderManager extends OrderManager {



	@Reference(timeout = 10000,group = "hltrip", check = false)
	private DfyOrderService dfyOrderService;

	@Autowired
	private ProductService productService;




	public final static String CHANNEL= ChannelConstant.SUPPLIER_TYPE_DFY_TOURS;
	public String getChannel(){
		return CHANNEL;
	}
	@Override
	public String test() {
		System.out.println("dfy_tours");
		return "dfy_tours";
	}


	public BaseResponse<OrderDetailRep> getOrderDetail(OrderOperReq req){
		BaseOrderRequest baseOrderRequest = new BaseOrderRequest();
		baseOrderRequest.setOrderId(req.getOrderId());
		baseOrderRequest.setTraceId(req.getTraceId());
		baseOrderRequest.setSupplierOrderId(req.getSupplierOrderId());
		BaseResponse<DfyToursOrderDetail> order = dfyOrderService.toursOrderDetail(baseOrderRequest);
		if(order==null)
			return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
		try {
			log.info("中台dfy跟团拿到的订单数据:"+ JSONObject.toJSONString(order));
			DfyToursOrderDetail dfyOrderDetail = order.getData();
			//如果数据为空,直接返回错
			if( !order.isSuccess())
				return BaseResponse.fail(CentralError.ERROR_NO_ORDER);//异常消息以供应商返回的
			OrderDetailRep rep=new OrderDetailRep();
			rep.setOrderId(dfyOrderDetail.getOrderId());
			//转换成consumer统一的订单状态
			rep.setOrderStatus(OrderInfoTranser.genCommonOrderStringStatus(dfyOrderDetail.getOrderStatus(),3));
			rep.setVochers(genVouchers(dfyOrderDetail));
			return BaseResponse.success(rep);
		} catch (Exception e) {
			log.error("中台dfy跟团报错:"+JSONObject.toJSONString(req),e);
			return BaseResponse.fail(9999,order.getMessage(),null);
		}

	}

	private List<OrderDetailRep.Voucher> genVouchers(DfyToursOrderDetail orderDetail) {

		try {
			if (CollectionUtils.isNotEmpty(orderDetail.getAttachments())) {
				List<OrderDetailRep.Voucher> vochers = new ArrayList<>();
				for (DfyToursOrderDetail.OrderAttachment oneInfo : orderDetail.getAttachments()) {
					//凭证类型   1.纯文本  2.二维码 3.PDF
					OrderDetailRep.Voucher oneVoucher = new OrderDetailRep.Voucher();
					oneVoucher.setVocherUrl(oneInfo.getUrl());
					oneVoucher.setType(3);
					vochers.add(oneVoucher);
				}
				return vochers;
			}
		} catch (Exception e) {
			log.error("genToursVouchers错", e);
		}
		return null;
	}

	public BaseResponse<OrderDetailRep> getVochers(OrderOperReq req){

		try {
			BaseOrderRequest baseOrderRequest = new BaseOrderRequest();
			baseOrderRequest.setOrderId(req.getOrderId());
			baseOrderRequest.setSupplierOrderId(req.getSupplierOrderId());
			baseOrderRequest.setTraceId(req.getTraceId());


			BaseResponse<DfyToursOrderDetail> order = dfyOrderService.toursOrderDetail(baseOrderRequest);
			if (order == null)
				return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
			log.info("getVochers中台dfy跟团拿到的订单数据:" + JSONObject.toJSONString(order));
			DfyToursOrderDetail dfyOrderDetail = order.getData();
			//如果数据为空,直接返回错
			if (!order.isSuccess())
				return BaseResponse.fail(CentralError.ERROR_NO_ORDER);//异常消息以供应商返回的
			OrderDetailRep rep = new OrderDetailRep();
			rep.setOrderId(dfyOrderDetail.getOrderId());
			//转换成consumer统一的订单状态
			rep.setOrderStatus(OrderInfoTranser.genCommonOrderStringStatus(dfyOrderDetail.getOrderStatus(), 3));
			rep.setVochers(genVouchers(dfyOrderDetail));

			return BaseResponse.success(rep);


		} catch (Exception e) {
			log.error("DfygetVochers跟团报错:"+JSONObject.toJSONString(req),e);
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
		DfyCreateToursOrderRequest dfyCreateOrderRequest = new DfyCreateToursOrderRequest();
		dfyCreateOrderRequest.setStartTime(req.getBeginDate());
		dfyCreateOrderRequest.setProductId(Integer.parseInt(req.getSupplierProductId()));
		dfyCreateOrderRequest.setAdultNum(req.getAdultNum());
		dfyCreateOrderRequest.setChildNum(req.getChildNum());
		dfyCreateOrderRequest.setStartCity(req.getStartCity());
		dfyCreateOrderRequest.setStartCityCode(req.getStartCityCode());
		dfyCreateOrderRequest.setStartTime(req.getBeginDate());

		//出行人
		List<CreateOrderReq.BookGuest> guests = req.getGuests();
		if(ListUtils.isNotEmpty(guests)) {
			List<ToursTourist> touristList = new ArrayList<>(guests.size());
			for (CreateOrderReq.BookGuest guest: guests) {
				ToursTourist tourist = new ToursTourist();
				tourist.setName(guest.getCname());
				tourist.setTel(guest.getMobile());
				//tourist.setFirstname();
				//tourist.setLastname();
				//tourist.setBirthday();
				//tourist.setPsptEndDate();
				//tourist.setSex();
				tourist.setTouristType(guest.getGuestType());
				int psptcode = guest.getCredentialType();
				int dfypscode = changecredentialType(psptcode);
				DfyCertificateType certificateByCode = DfyCertificateType.getCertificateByCode(dfypscode);
				if (certificateByCode != null) {
					tourist.setPaper_num(guest.getCredential());
					tourist.setPaper_type(certificateByCode.getCode());
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
		DfyBaseResult<DfyCreateOrderResponse> order = dfyOrderService.createToursOrder(dfyCreateOrderRequest);
		if(order != null && order.getStatusCode() == 200 && order.getData() != null) {
			CenterCreateOrderRes createOrderRes = new CenterCreateOrderRes();
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
		/*DfyPayOrderRequest request = new DfyPayOrderRequest();
		request.setTraceId(req.getTraceId());
		request.setChannelCode(req.getChannelCode());
		request.setChannelOrderId(req.getChannelOrderId());
		request.setPrice(String.valueOf(req.getPrice()));*/
		//DfyBaseResult dfyBaseResult = dfyOrderService.payOrder(request);
		//if(dfyBaseResult != null && dfyBaseResult.isSuccess()){
			CenterPayOrderRes payOrderRes = new CenterPayOrderRes();
			payOrderRes.setChannelOrderId(req.getChannelOrderId());
			payOrderRes.setLocalOrderId(req.getPartnerOrderId());
			payOrderRes.setOrderStatus(10);
			return BaseResponse.success(payOrderRes);
		//}
		//return BaseResponse.fail(CentralError.ERROR_ORDER_PAY);
	}


	/**
	 * 取消订单
	 * @param req
	 * @return
	 */
	public  BaseResponse<CenterCancelOrderRes> getCenterCancelOrder(CancelOrderReq req){
		CenterCancelOrderRes centerCancelOrderRes = new CenterCancelOrderRes();
		centerCancelOrderRes.setOrderStatus(OrderStatus.APPLYING_FOR_REFUND.getCode());
		return BaseResponse.success(centerCancelOrderRes);

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
	 * 申请退款
	 * @param req
	 * @return
	 */
	public BaseResponse<CenterCancelOrderRes> getCenterApplyRefund(CancelOrderReq req){
		CenterCancelOrderRes centerCancelOrderRes = new CenterCancelOrderRes();
		centerCancelOrderRes.setOrderStatus(OrderStatus.APPLYING_FOR_REFUND.getCode());
		return BaseResponse.success(centerCancelOrderRes);

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
