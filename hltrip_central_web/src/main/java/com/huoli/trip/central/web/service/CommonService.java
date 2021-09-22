package com.huoli.trip.central.web.service;

import com.huoli.trip.common.vo.IncreasePrice;
import com.huoli.trip.common.vo.IncreasePriceCalendar;

import java.util.List;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2021/4/27<br>
 */
public interface CommonService {

    /**
     * 加价，价格日历加价用这个
     * @param increasePrice
     */
    void increasePrice(IncreasePrice increasePrice);

    /**
     * 加价，单日加价用这个方法
     * @param increasePrice
     */
    void increasePriceByPackageId(IncreasePrice increasePrice);
}
