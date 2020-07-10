package com.huoli.trip.central.api;

import com.huoli.trip.common.vo.ImageBase;
import com.huoli.trip.common.vo.request.central.*;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.central.*;

import java.util.List;


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

    /**
     * 获取图片
     * @param request
     * @return
     */
    BaseResponse<List<ImageBase>> getImages(ImageRequest request);

    /**
     * 套餐价格详情
     */
    BaseResponse<ProductPriceDetialResult> getPriceDetail(ProductPriceReq req);

    /**
     * 计算价格
     * @param request
     * @return
     */
    BaseResponse<PriceCalcResult> calcTotalPrice(PriceCalcRequest request);
}
