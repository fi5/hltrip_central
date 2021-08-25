package com.huoli.trip.central.web.mapper;

import com.huoli.trip.common.entity.TripPromotion;
import com.huoli.trip.common.vo.response.promotion.PromotionDetailResult;
import com.huoli.trip.common.vo.response.promotion.PromotionListResult;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripPromotionMapper {

    @Select("select id as promotionId,discount_type,title,tips,discount,brief_desc,image from trip_promotion")
    List<PromotionListResult> getList();

    @Select("select id as promotionId,discount_type,title,tips,discount,detail_desc,image,rule_desc,assist_num,assist_times from trip_promotion")
    PromotionDetailResult getResultById(long id);

    @Select("select * from trip_promotion where id=#{id}")
    TripPromotion getById(long id);

}
