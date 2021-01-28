package com.huoli.trip.central.web.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.huoli.trip.central.api.ProductService;
import com.huoli.trip.central.web.converter.OrderInfoTranser;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.constant.ChannelConstant;
import com.huoli.trip.common.vo.request.OrderOperReq;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.order.OrderDetailRep;
import com.huoli.trip.supplier.api.DfyOrderService;
import com.huoli.trip.supplier.self.difengyun.DfyOrderDetail;
import com.huoli.trip.supplier.self.difengyun.vo.DfyToursOrderDetail;
import com.huoli.trip.supplier.self.yaochufa.vo.BaseOrderRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
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

}
