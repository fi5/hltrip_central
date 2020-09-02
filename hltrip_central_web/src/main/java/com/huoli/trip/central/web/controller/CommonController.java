package com.huoli.trip.central.web.controller;

import com.huoli.eagle.eye.core.HuoliTrace;
import com.huoli.trip.central.web.service.OrderFactory;
import com.huoli.trip.central.web.service.impl.OrderManager;
import com.huoli.trip.common.vo.response.BaseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2020/9/2<br>
 */
@RestController
@RequestMapping(value = "/common", produces = "application/json")
public class CommonController {

    @Autowired
    private OrderFactory orderFactory;

    @Autowired
    private HuoliTrace huoliTrace;

    @PostMapping("/sync/price")
    public BaseResponse syncPrice(@RequestParam
                                      @NotBlank(message = "产品编码productCode不能为空") String productCode,
                                  String supplierProductCode,
                                  @NotBlank(message = "开始日期startDate不能为空") String startDate,
                                  String endDate,
                                  @NotBlank(message = "渠道channel不能为空") String channel){
        OrderManager orderManager = orderFactory.getOrderManager(channel);
        orderManager.syncPrice(productCode, supplierProductCode, startDate, endDate, huoliTrace.getTraceInfo().getTraceId());
        return BaseResponse.withSuccess();
    }
}
