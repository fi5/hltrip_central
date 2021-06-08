package com.huoli.trip.central.web.mapper;

import com.huoli.trip.common.entity.po.PassengerTemplatePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2021/6/8<br>
 */
@Repository
//@Mapper
public interface PassengerTemplateMapper {

    /**
     * 根据id获取出行人模板
     * @param id
     * @return
     */
    @Select("select peopleLimit, passengerInfo, idInfo from trip_passenger_template where id = #{id}")
    PassengerTemplatePO getPassengerTemplateById(int id);
}
