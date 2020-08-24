package com.huoli.trip.central.web.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.huoli.trip.central.api.ProductService;
import com.huoli.trip.common.vo.request.central.ProductPageRequest;
import com.huoli.trip.common.vo.response.BaseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2020/8/24<br>
 */
@RestController
@RequestMapping("/mongo")
public class MongoToolController {

    @Autowired
    private ProductService productService;

    @PostMapping("/product")
    public BaseResponse<Object> getProduct(@RequestBody Map<String, Object> cond){
        ProductPageRequest request = JSONObject.toJavaObject(JSON.parseObject(JSONObject.toJSONString(cond)), ProductPageRequest.class);
        return BaseResponse.withSuccess(productService.pageList(request));
    }
}
