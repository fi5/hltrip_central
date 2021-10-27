package com.huoli.trip.central.web.mapper;

import com.huoli.trip.common.entity.TripOrder;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2021/9/23<br>
 */
@Repository
public interface TripOrderMapper {

    @Select("select quantity, productId from trip_order where orderId = #{orderId}")
    TripOrder getOrderById(String orderId);
}
