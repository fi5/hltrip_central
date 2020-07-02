package com.huoli.trip.central.api;

import com.huoli.trip.common.vo.Product;
import com.huoli.trip.common.vo.ProductPageRequest;
import com.huoli.trip.common.vo.ProductPageResult;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.ListResult;

import java.util.List;

/**
 * 产品列表，产品详情相关的接口服务
 */
public interface ProductService {

    ListResult mainList(String city, Integer type, Integer mainPageSize);

    BaseResponse<List<Product>> pageList(ProductPageRequest request);

}
