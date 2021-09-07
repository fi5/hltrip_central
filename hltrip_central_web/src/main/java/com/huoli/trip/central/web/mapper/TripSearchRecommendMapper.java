package com.huoli.trip.central.web.mapper;

import com.huoli.trip.common.entity.TripSearchRecommend;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripSearchRecommendMapper {

    @Select("select * from trip_search_recommend where position=#{position}")
    List<TripSearchRecommend> listByPosition(int position);

    @Select("select * from trip_search_recommend where position=#{position} and cityCode=#{cityCode}")
    List<TripSearchRecommend> listByPositionAndCityCode(int position, String cityCode);
}
