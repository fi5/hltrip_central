package com.huoli.trip.central.api;

import com.huoli.trip.common.vo.request.central.CategoryDetailRequest;
import com.huoli.trip.common.vo.request.central.ProductPageRequest;
import com.huoli.trip.common.vo.request.central.ProductPriceReq;
import com.huoli.trip.common.vo.request.central.RecommendRequest;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.central.*;


/**
 * 产品列表，产品详情相关的接口服务
 */
public interface ProductService {

    /**
     * 商品列表
     * @param request
     * @return
     */
    BaseResponse<ProductPageResult> pageList(ProductPageRequest request);

    /**
     * 商品详情
     * @param request
     * @return
     */
    BaseResponse<CategoryDetailResult> categoryDetail(CategoryDetailRequest request);

    /**
     * 推荐列表
     * @param request
     * @return
     */
    BaseResponse<RecommendResult> recommendList(RecommendRequest request);

	/**
     * 价格日历
     * @param productPriceReq
     * @return
     */
    BaseResponse<ProductPriceCalendarResult> productPriceCalendar(ProductPriceReq productPriceReq);

    BaseResponse<ProductPriceDetialResult> priceDetail(ProductPriceReq productPriceReq);
}
