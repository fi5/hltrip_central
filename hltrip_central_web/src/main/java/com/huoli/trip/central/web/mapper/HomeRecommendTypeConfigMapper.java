package com.huoli.trip.central.web.mapper;

import com.huoli.trip.common.vo.response.recommend.HomeRecommendTypeRes;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HomeRecommendTypeConfigMapper {

    @Select("select id,type as name from home_recommend_type_config where status=#{status} order by sorting")
    List<HomeRecommendTypeRes> list(int status);

}
