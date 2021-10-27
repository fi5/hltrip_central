package com.huoli.trip.central.web.mapper;

import com.huoli.trip.common.entity.TripPromotion;
import com.huoli.trip.common.vo.response.promotion.PromotionDetailResult;
import com.huoli.trip.common.vo.response.promotion.PromotionListResult;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripPromotionMapper {

    @Select("select id as promotionId,discount_type,title,tips,discount,brief_desc,image,valid_time from trip_promotion where status=#{status}")
    List<PromotionListResult> getList(int status);

    @Select("select id as promotionId,discount_type,title,tips,discount,detail_desc,image,rule_desc,assist_num,assist_times,active_flag,valid_time,status from trip_promotion where id=#{id} and status=#{status}")
    PromotionDetailResult getResultById(long id, int status);

    @Select("select * from trip_promotion where id=#{id} and status=#{status}")
    TripPromotion getById(long id, int status);

}
