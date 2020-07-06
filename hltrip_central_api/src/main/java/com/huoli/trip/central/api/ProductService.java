package com.huoli.trip.central.api;

import com.huoli.trip.common.vo.PriceInfo;
import com.huoli.trip.common.vo.Product;
import com.huoli.trip.common.vo.request.central.CategoryDetailRequest;
import com.huoli.trip.common.vo.request.central.ProductPageRequest;
import com.huoli.trip.common.vo.request.central.ProductPriceReq;
import com.huoli.trip.common.vo.request.central.RecommendRequest;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.ListResult;
import com.huoli.trip.common.vo.response.central.CategoryDetailResult;
import com.huoli.trip.common.vo.response.central.ProductPageResult;
import com.huoli.trip.common.vo.response.central.ProductPriceResult;

import java.util.List;


/**
 * 产品列表，产品详情相关的接口服务
 */
public interface ProductService {

    ListResult mainList(String city, Integer type, Integer mainPageSize);

    BaseResponse<ProductPageResult> pageList(ProductPageRequest request);

    BaseResponse<CategoryDetailResult> categoryDetail(CategoryDetailRequest request);

    BaseResponse<ProductPageResult> recommendList(RecommendRequest request);

	/**
     * 价格日历
     * @param productPriceReq
     * @return
     */
    BaseResponse<ProductPriceResult> productPriceCalendar(ProductPriceReq productPriceReq);
}
