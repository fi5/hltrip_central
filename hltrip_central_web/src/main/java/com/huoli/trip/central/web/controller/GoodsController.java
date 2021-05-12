package com.huoli.trip.central.web.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.huoli.trip.central.api.ProductService;
import com.huoli.trip.common.vo.request.RefundNoticeReq;
import com.huoli.trip.common.vo.request.central.ProductPageRequest;
import com.huoli.trip.common.vo.request.goods.GroupTourListReq;
import com.huoli.trip.common.vo.request.goods.HotelScenicListReq;
import com.huoli.trip.common.vo.request.goods.ScenicTicketListReq;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.goods.GroupTourListResult;
import com.huoli.trip.common.vo.response.goods.HotelScenicListResult;
import com.huoli.trip.common.vo.response.goods.ScenicTicketListResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author zhouwenbin
 * @version 1.0
 * @date 2021/4/27
 */
@RestController
@RequestMapping(value = "/goods", produces = "application/json")
@Slf4j
public class GoodsController {

    @Autowired
    private ProductService productService;


    @PostMapping("/scenicTicketList")
    public BaseResponse scenicTicketList(@RequestBody ScenicTicketListReq req){
        ScenicTicketListResult result= productService.scenicTicketList(req);
        return BaseResponse.withSuccess(result);
    }

    @PostMapping("/groupTourList")
    public BaseResponse groupTourList(@RequestBody GroupTourListReq req){
        GroupTourListResult result= productService.groupTourList(req);
        return BaseResponse.withSuccess(result);
    }

    @PostMapping("/hotelScenicList")
    public BaseResponse hotelScenicList(@RequestBody HotelScenicListReq req){
        HotelScenicListResult result= productService.hotelScenicList(req);
        return BaseResponse.withSuccess(result);
    }
}
