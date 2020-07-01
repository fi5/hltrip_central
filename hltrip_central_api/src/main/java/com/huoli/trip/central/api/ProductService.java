package com.huoli.trip.central.api;

import com.huoli.trip.common.vo.Product;

import java.util.List;

/**
 * 产品列表，产品详情相关的接口服务
 */
public interface ProductService {

    List<Product> productList(String city, Integer type, Integer mainPageSize);

}
