package com.huoli.trip.central.web.controller;

import com.huoli.trip.common.vo.request.RefundNoticeReq;
import com.huoli.trip.common.vo.response.BaseResponse;
import org.springframework.validation.annotation.Validated;
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
public class RecSupplierController {



	@RequestMapping(value = "/refundNotice", method = RequestMethod.POST)
	public BaseResponse refundNotice(@RequestBody RefundNoticeReq req) {
		return BaseResponse.success(null);
	}
}
