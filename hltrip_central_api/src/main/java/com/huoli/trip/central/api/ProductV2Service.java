package com.huoli.trip.central.api;

import com.huoli.trip.common.entity.mpo.groupTour.GroupTourPrice;
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

    ScenicSpotBase querycScenicSpotBase(ScenicSpotRequest request);

    GroupTourBody queryGroupTourById(GroupTourRequest request);
    GroupMealsBody groupMealsBody(GroupTourMealsRequest request);

    List<ScenicSpotProductBase> queryScenicSpotProduct(ScenicSpotProductRequest request);

    List<BasePrice> queryCalendar(CalendarRequest request);

    BaseResponse<ProductPriceCalendarResult> queryGroupTourPriceCalendar(CalendarRequest request);

}
