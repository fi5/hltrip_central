package com.huoli.trip.central.web.mapper;

import com.huoli.trip.common.entity.TripPromotionInvitation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectKey;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.mapping.StatementType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface TripPromotionInvitationMapper {

    @Select("select * from trip_promotion_invitation where phone_id=#{phoneId} and promotion_id=#{promotionId} order by create_time desc")
    List<TripPromotionInvitation> getByPhoneIdPromotionId(String phoneId, long promotionId);

    @Select("select * from trip_promotion_invitation where id=#{id}")
    TripPromotionInvitation getById(long id);

    @Select("select ifnull(invite_num,0) from trip_promotion_invitation where id=#{id}")
    Integer getInviteNumById(long id);



    @Insert("insert into trip_promotion_invitation (phone_id,promotion_id,assist_num,invite_num,valid_time,timer,status,create_time,update_time,is_first)" +
            " values (#{phoneId},#{promotionId},#{assistNum},#{inviteNum},#{validTime},#{timer},#{status},#{createTime},#{updateTime},#{isFirst})")
    @SelectKey(before = false, keyProperty = "id", resultType = Long.class, statement = "SELECT last_insert_id() as id", statementType = StatementType.STATEMENT)
    Long insert(TripPromotionInvitation tripPromotionInvitation);

    @Update("update trip_promotion_invitation set invite_num=#{newInviteNum} where id=#{id} and invite_num=#{oldInviteNum}")
    void updateInviteNum(long id, int newInviteNum, int oldInviteNum);

    @Update("update trip_promotion_invitation set invite_num=#{newInviteNum},coupon_status=#{newCouponStatus} where id=#{id} and invite_num=#{inviteNum} and coupon_status=#{oldCouponStatus}")
    void updateInviteNumAndCouponStatus(long id, int newInviteNum, int inviteNum, int newCouponStatus, int oldCouponStatus);

    @Update("update trip_promotion_invitation set coupon_status=#{newCouponStatus},is_first=#{newIsFirst},status=#{newStatus} where id=#{id} and coupon_status=#{oldCouponStatus} and is_first=#{oldIsFirst} and status=#{oldStatus}")
    void updateCouponStatus(long id, int newCouponStatus, int oldCouponStatus, String newIsFirst, String oldIsFirst, int newStatus, int oldStatus);

    @Select("select ifnull(count(1),0) from trip_promotion_invitation where phone_id=#{phoneId} and promotion_id=#{promotionId}")
    int countByPhoneIdAndId(String phoneId, long promotionId);

    @Select("select ifnull(count(1),0) from trip_promotion_invitation where phone_id=#{phoneId} and promotion_id=#{promotionId} and timer>#{timer}")
    int countByPhoneIdAndIdAndTime(String phoneId, long promotionId, long timer);

    @Select("select id from trip_promotion_invitation where phone_id=#{phoneId} and promotion_id=#{promotionId} order by create_time desc limit 1")
    long getIdByPhoneId(String phoneId, long promotionId);

    @Update("update trip_promotion_invitation set is_first=#{newIsFirst} where id=#{id} and is_first=#{oldIsFirst}")
    void updateIsFirst(long id, String newIsFirst, String oldIsFirst);

    @Update("update trip_promotion_invitation set status=#{status} where id=#{id}")
    void updateStatus(long id, int status);

    @Select("select * from trip_promotion_invitation where phone_id=#{phoneId} and promotion_id=#{promotionId} order by create_time desc limit 1")
    TripPromotionInvitation getOneByPhoneIdPromotionId(String phoneId, long promotionId);
}
