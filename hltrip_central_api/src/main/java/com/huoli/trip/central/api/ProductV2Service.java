package com.huoli.trip.central.api;

import com.huoli.trip.common.vo.request.v2.CalendarRequest;
import com.huoli.trip.common.vo.request.v2.ScenicSpotProductRequest;
import com.huoli.trip.common.vo.v2.BasePrice;
import com.huoli.trip.common.vo.v2.ScenicSpotBase;
import com.huoli.trip.common.vo.request.v2.ScenicSpotRequest;
import com.huoli.trip.common.vo.v2.ScenicSpotProductBase;

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

    List<ScenicSpotProductBase> queryScenicSpotProduct(ScenicSpotProductRequest request);

    List<BasePrice> queryCalendar(CalendarRequest request);

}
