package com.huoli.trip.central.api;

import com.huoli.trip.common.entity.mpo.AddressInfo;
import com.huoli.trip.common.entity.mpo.ProductListMPO;
import com.huoli.trip.common.vo.ImageBase;
import com.huoli.trip.common.vo.IncreasePrice;
import com.huoli.trip.common.vo.Product;
import com.huoli.trip.common.vo.request.GroupTourSearchReq;
import com.huoli.trip.common.vo.request.HomeSearchReq;
import com.huoli.trip.common.vo.request.TicketSearchReq;
import com.huoli.trip.common.vo.request.central.*;
import com.huoli.trip.common.vo.request.goods.GroupTourListReq;
import com.huoli.trip.common.vo.request.goods.HotelScenicListReq;
import com.huoli.trip.common.vo.request.goods.ScenicTicketListReq;
import com.huoli.trip.common.vo.request.promotion.*;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.central.*;
import com.huoli.trip.common.vo.response.goods.GroupTourListResult;
import com.huoli.trip.common.vo.response.goods.HotelScenicListResult;
import com.huoli.trip.common.vo.response.goods.ScenicTicketListResult;
import com.huoli.trip.common.vo.response.recommend.*;
import com.huoli.trip.common.vo.response.promotion.PromotionDetailResult;
import com.huoli.trip.common.vo.response.promotion.PromotionListResult;
import com.huoli.trip.common.vo.response.recommend.RecommendResultV2;

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
     * item列表
     * @param request
     * @return
     */
    BaseResponse<ProductPageResult> pageListForProduct(ProductPageRequest request);

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

    /**
     * 预览
     * @param request
     * @return
     */
    BaseResponse<CategoryDetailResult> previewDetail(PreviewDetailRequest request);

    /**
     * 获取推荐产品
     * @param size
     * @return
     */
    List<Product> getFlagRecommendProducts(Integer productType, int size);

    List<Product> getFlagRecommendProducts(int size);

    /**
     * 获取推荐产品列表
     * @param request
     * @return
     */
    BaseResponse<RecommendResult> recommendListV2(RecommendRequest request);

    /**
     * 门票列表
     * [req]
     * @return {@link BaseResponse< ScenicTicketListResult>}
     * @throws
     */
    BaseResponse<ScenicTicketListResult> scenicTicketList(ScenicTicketListReq req);

    /**
     * 跟团游列表
     * [req]
     * @return {@link BaseResponse< ScenicTicketListResult>}
     * @throws
     */
    BaseResponse<GroupTourListResult> groupTourList(GroupTourListReq req);

    /**
     * 酒景列表
     * [req]
     * @return {@link BaseResponse< ScenicTicketListResult>}
     * @throws
     */
    BaseResponse<HotelScenicListResult> hotelScenicList(HotelScenicListReq req);

    IncreasePrice increasePrice(ProductListMPO productListMPO, String app, String source);

    /**
     * 推荐列表
     * @param request
     * @return
     */
    BaseResponse<RecommendResultV2> recommendListV3(RecommendRequestV2 request);

    /**
     * 推荐标签
     * @param request
     * @return
     */
    BaseResponse<List<String>> recommendTags(RecommendRequestV2 request);

    /**
     * 热门城市
     * @param request
     * @return
     */
    BaseResponse<List<AddressInfo>> recommendCites(RecommendRequestV2 request);

    /**
     * 推荐主题
     * @param request
     * @return
     */
    BaseResponse<List<String>> recommendSubjects(RecommendRequestV2 request);

    /**
     * 新版价格计算
     * @param request
     * @return
     */
    BaseResponse<PriceCalcResult> calcTotalPriceV2(PriceCalcRequest request);

    /**
     * 新版价格详情
     * @param req
     * @return
     */
    BaseResponse<ProductPriceDetailResultV2> getPriceDetailV2(ProductPriceReq req);

    /**
     * 助力折扣列表
     * @param request
     * @return
     */
    BaseResponse<List<PromotionListResult>> tripPromotionList(PromotionListReq request);

    /**
     * 助力折扣详情
     * @param request
     * @return
     */
    BaseResponse<PromotionDetailResult> tripPromotionDetail(PromotionDetailReq request);

    /**
     * 发起助力折扣邀请
     * @param request
     * @return
     */
    BaseResponse<String> tripPromotionInvitation(PromotionInvitationReq request);

    /**
     * 检查优惠券是否可以发放
     * @param req
     * @return
     */
    BaseResponse<String> tripPromotionCheckCoupon(CouponSendReq req);

    /**
     * 助力
     * @param req
     * @return
     */
    BaseResponse<String> acceptPromotionInvitation(AcceptPromotionInvitationReq req);

    /**
     * 首页和综合页搜索默认推荐
     * @param req
     * @return
     */
    BaseResponse<List<HomeRecommendRes>> homeSearchDefaultRecommend(HomeSearchReq req);

    /**
     * 首页和综合页搜索推荐
     * @param req
     * @return
     */
    BaseResponse<List<HomeSearchRes>> homeSearchRecommend(HomeSearchReq req);

    /**
     * 门票搜索默认推荐
     *
     * @return
     */
    BaseResponse<List<ScenicSpotProductSearchRecommendRes>> scenicSpotProductSearchDefaultRecommend(TicketSearchReq req);

    /**
     * 门票搜索推荐
     * @param req
     * @return
     */
    BaseResponse<List<ScenicSpotProductSearchRes>> scenicSpotProductSearchRecommend(HomeSearchReq req);

    /**
     * 跟团游搜索默认推荐
     * @param req
     * @return
     */
    BaseResponse<List<GroupTourRecommendRes>> groupTourSearchDefaultRecommend(GroupTourSearchReq req);

    /**
     * 跟团游搜索推荐
     * @param req
     * @return
     */
    BaseResponse<List<HomeSearchRes>> groupTourSearchRecommend(HomeSearchReq req);

    BaseResponse getAllCity();
}
