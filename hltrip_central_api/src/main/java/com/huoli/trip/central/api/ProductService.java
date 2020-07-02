package com.huoli.trip.central.api;

import com.huoli.trip.common.vo.response.ListResult;

/**
 * 产品列表，产品详情相关的接口服务
 */
public interface ProductService {

    ListResult productList(String city, Integer type, Integer mainPageSize);

}
