package com.huoli.trip.central.web.mapper;

import com.huoli.trip.common.entity.TicketRecommendConfig;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRecommendConfigMapper {

    @Select("select * from ticket_recommend_config where status=#{status} order by sorting")
    List<TicketRecommendConfig> list(int status);
}
