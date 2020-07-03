package com.huoli.trip.central.api;

import com.huoli.trip.common.vo.request.CategoryDetailRequest;
import com.huoli.trip.common.vo.request.ProductPageRequest;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.CategoryDetailResult;
import com.huoli.trip.common.vo.response.ListResult;
import com.huoli.trip.common.vo.response.ProductPageResult;


/**
 * 产品列表，产品详情相关的接口服务
 */
public interface ProductService {

    ListResult mainList(String city, Integer type, Integer mainPageSize);

    BaseResponse<ProductPageResult> pageList(ProductPageRequest request);

    BaseResponse<CategoryDetailResult> categoryDetail(CategoryDetailRequest request);

}
