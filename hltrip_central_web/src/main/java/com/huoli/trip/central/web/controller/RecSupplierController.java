package com.huoli.trip.central.web.controller;

import com.huoli.trip.central.api.OrderService;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.vo.request.PushOrderStatusReq;
import com.huoli.trip.common.vo.request.RefundNoticeReq;
import com.huoli.trip.common.vo.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author :zhouwenbin
 * @time   :2020/7/1
 * @comment:
 **/
@RestController
@RequestMapping(value = "/recSupplier")
@Slf4j
public class RecSupplierController {

	@Autowired
	private OrderService orderService;

	/**
	 * 接收退款通知
	 * @param req
	 * @return
	 */
	@RequestMapping(value = "/refundNotice", method = RequestMethod.POST)
	public BaseResponse refundNotice(@RequestBody RefundNoticeReq req) {
		orderService.refundNotice(req);
		return BaseResponse.success(null);
	}

	@RequestMapping(value = "/orderStatusNotice", method = RequestMethod.POST)
	public BaseResponse orderStatusNotice(@RequestBody PushOrderStatusReq req) {
		orderService.orderStatusNotice(req);
		return BaseResponse.success(null);
	}
}
