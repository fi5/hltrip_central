package com.huoli.trip.central.web.dao;

import com.huoli.trip.common.entity.mpo.hotelScenicSpot.HotelScenicSpotProductMPO;
import com.huoli.trip.common.entity.mpo.hotelScenicSpot.HotelScenicSpotProductSetMealMPO;
import com.huoli.trip.common.vo.request.v2.CalendarRequest;
import com.huoli.trip.common.vo.request.v2.HotelScenicSetMealRequest;
import com.huoli.trip.common.vo.v2.HotelScenicProductDetail;

import java.util.List;

/**
 * @program: hltrip
 * @description: 酒景套餐
 * @author: WangYing
 * @create: 2021-05-25 16:55
 **/
public interface HotelScenicDao {
    HotelScenicProductDetail queryHotelScenicProductById(String productId);

    List<HotelScenicSpotProductSetMealMPO> queryHotelScenicSetMealList(CalendarRequest request);

    HotelScenicSpotProductMPO queryHotelScenicProductMpoById(String productId);

    HotelScenicSpotProductSetMealMPO queryHotelScenicsetMealById(HotelScenicSetMealRequest request);
}
