package com.huoli.trip.central.web.mapper;

import com.huoli.trip.common.entity.TripPromotionInvitationAccept;
import com.huoli.trip.common.vo.response.promotion.PromotionDetailResult;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface TripPromotionInvitationAcceptMapper {

    @Select("SELECT" +
            " a.invitee_avatar avatar,a.invitee_nickname nickname" +
            " FROM" +
            " trip_promotion_invitation i" +
            " LEFT JOIN trip_promotion_invitation_accept a ON i.id = a.invitation_id" +
            " WHERE" +
            " i.phone_id = #{phoneId} " +
            " AND i.promotion_id = #{promotionId}")
    List<PromotionDetailResult.Friend> getFriends(String phoneId, long promotionId);

    @Select("select * from trip_promotion_invitation_accept where invitation_id=#{id} and invitee_phone_id=#{phoneId}")
    TripPromotionInvitationAccept getByInvitationIdAndPhoneId(long id, String phoneId);

    @Select("select ifnull(count(1),0) from trip_promotion_invitation_accept where invitee_phone_id=#{phoneId} and create_time between #{start} and #{end}")
    Integer countByPhoneId(String phoneId, Date start, Date end);

    @Insert("insert into trip_promotion_invitation_accept (invitationId,inviteePhoneId,inviteeNickname,inviteeAvatar,createTime,updateTime)" +
            " values (#{invitationId},#{inviteePhoneId},#{inviteeNickname},#{inviteeAvatar},#{createTime},#{updateTime})")
    void insert(TripPromotionInvitationAccept accept);

    @Select("select ifnull(count(1),0) from trip_promotion_invitation_accept where invitation_id=#{invitationId}")
    Integer countByInvitationId(long invitationId);

}
