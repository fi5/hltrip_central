package com.huoli.trip.central.api;

import com.huoli.trip.common.vo.request.v2.*;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.central.ProductPriceCalendarResult;
import com.huoli.trip.common.vo.v2.*;

import java.util.List;

/**
 * @author lunatic
 * @Title:
 * @Package
 * @Description:
 * @date 2021/4/2615:37
 */
public interface ProductV2Service {

    BaseResponse<ScenicSpotBase> querycScenicSpotBase(ScenicSpotRequest request);

    BaseResponse<GroupTourBody> queryGroupTourById(GroupTourRequest request);
    GroupMealsBody groupMealsBody(GroupTourMealsRequest request);

    BaseResponse<List<ScenicSpotProductBase>> queryScenicSpotProduct(ScenicSpotProductRequest request);

    BaseResponse<List<BasePrice>> queryCalendar(CalendarRequest request);

    BaseResponse<ProductPriceCalendarResult> queryGroupTourPriceCalendar(CalendarRequest request);
    BaseResponse<ScenicSpotProductDetail> queryScenicSpotProductDetail(ScenicSpotProductRequest request);

    BaseResponse<HotelScenicProductDetail> hotelScenicProductDetail(HotelScenicProductRequest request);

    BaseResponse<List<HotelScenicProductSetMealBrief>> hotelScenicProductSetMealList(CalendarRequest request);

    BaseResponse<ProductPriceCalendarResult> queryHotelScenicPriceCalendar(CalendarRequest request);

    BaseResponse<HotelScenicProductSetMealDetail> queryHotelScenicSetMealDetail(HotelScenicSetMealRequest request);

    BaseResponse<HotelInfoBase> queryHotelInfoDetail(HotelInfoRequest request);

    BaseResponse<List<ScenicRealProductBase>> querySpotRelaProductList(ScenicSpotProductRequest request);
}
