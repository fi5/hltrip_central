package com.huoli.trip.central.api;

import com.huoli.trip.common.vo.Product;
import com.huoli.trip.common.vo.response.ListResult;

import java.util.List;

/**
 * 产品列表，产品详情相关的接口服务
 */
public interface ProductService {

    ListResult mainList(String city, Integer type, Integer mainPageSize);

    List<Product> pageList(String city, Integer type, int pageIndex, int pageSize);

}
