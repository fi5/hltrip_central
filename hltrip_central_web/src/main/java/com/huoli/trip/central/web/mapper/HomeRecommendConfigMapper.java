package com.huoli.trip.central.web.mapper;

import com.huoli.trip.common.entity.HomeRecommendConfig;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HomeRecommendConfigMapper {

    @Select("select * from Home_recommend_config where status=#{status} order by sorting")
    List<HomeRecommendConfig> list(int status);
}
