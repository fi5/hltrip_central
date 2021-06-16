package com.huoli.trip.central.web.controller;

import com.huoli.trip.central.api.ProductService;
import com.huoli.trip.common.vo.request.goods.GroupTourListReq;
import com.huoli.trip.common.vo.request.goods.HotelScenicListReq;
import com.huoli.trip.common.vo.request.goods.ScenicTicketListReq;
import com.huoli.trip.common.vo.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


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
        return productService.scenicTicketList(req);
    }

    @PostMapping("/groupTourList")
    public BaseResponse groupTourList(@RequestBody GroupTourListReq req){
        return productService.groupTourList(req);
    }

    @PostMapping("/hotelScenicList")
    public BaseResponse hotelScenicList(@RequestBody HotelScenicListReq req){
        return productService.hotelScenicList(req);
    }
}
