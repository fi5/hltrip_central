package com.huoli.trip.central.api;

import com.huoli.trip.common.vo.v2.ScenicSpotBase;
import com.huoli.trip.common.vo.request.ScenicSpotRequest;

/**
 * @author lunatic
 * @Title:
 * @Package
 * @Description:
 * @date 2021/4/2615:37
 */
public interface ProductV2Service {

    ScenicSpotBase querycScenicSpotBase(ScenicSpotRequest request);

}
