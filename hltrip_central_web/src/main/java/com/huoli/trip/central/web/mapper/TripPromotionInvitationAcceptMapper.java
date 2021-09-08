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

    @Select("SELECT invitee_avatar avatar,invitee_nickname nickname FROM trip_promotion_invitation_accept WHERE invitation_id=#{invitationId}")
    List<PromotionDetailResult.Friend> getFriends(long invitationId);

    @Select("select * from trip_promotion_invitation_accept where invitation_id=#{id} and invitee_phone_id=#{phoneId}")
    TripPromotionInvitationAccept getByInvitationIdAndPhoneId(long id, String phoneId);

    @Select("select ifnull(count(1),0) from trip_promotion_invitation_accept where invitee_phone_id=#{phoneId} and create_time between #{start} and #{end}")
    Integer countByPhoneId(String phoneId, Date start, Date end);

    @Insert("insert into trip_promotion_invitation_accept (invitation_id,invitee_phone_id,invitee_nickname,invitee_avatar,create_time,update_time)" +
            " values (#{invitationId},#{inviteePhoneId},#{inviteeNickname},#{inviteeAvatar},#{createTime},#{updateTime})")
    void insert(TripPromotionInvitationAccept accept);

    @Select("select ifnull(count(1),0) from trip_promotion_invitation_accept where invitation_id=#{invitationId}")
    Integer countByInvitationId(long invitationId);

}
