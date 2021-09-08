package com.huoli.trip.central.web.mapper;

import com.huoli.trip.common.entity.TripSearchRecommendDetail;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripSearchRecommendMapper {

    @Select("SELECT d.* FROM trip_search_recommend r LEFT JOIN trip_search_recommend_detail d ON r.id=d.recommendId WHERE r.position=#{position}")
    List<TripSearchRecommendDetail> listByPosition(int position);

    @Select("SELECT d.* FROM trip_search_recommend r LEFT JOIN trip_search_recommend_detail d ON r.id=d.recommendId WHERE r.position=#{position} and r.contactCityCode=#{cityCode}")
    List<TripSearchRecommendDetail> listByPositionAndCityCode(int position, String cityCode);

    @Select("SELECT contactAreaCode FROM trip_search_recommend WHERE position=#{position}")
    List<String> getContactAreaCode(int position);

    @Select("SELECT d.* FROM trip_search_recommend r LEFT JOIN trip_search_recommend_detail d on r.id=d.recommendId WHERE r.position=#{position} and r.contactAreaCode=#{contactAreaCode}")
    List<TripSearchRecommendDetail> listByContactAreaCode(int position, String contactAreaCode);
}
