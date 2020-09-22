package com.huoli.trip.central.web.dao;

import com.huoli.trip.common.entity.HodometerPO;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2020/9/18<br>
 */
public interface HodometerDao {

    /**
     * 获取产品行程
     * @param productCode
     * @return
     */
    HodometerPO getHodometerByProductCode(String productCode);
}
