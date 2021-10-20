package com.huoli.trip.central.web.dao;

import com.huoli.trip.common.entity.mpo.scenicSpotTicket.ScenicSpotProductMPO;

import java.util.List;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2021/5/31<br>
 */
public interface ScenicSpotProductDao {

    /**
     * 根据id获取产品
     * @param productId
     * @return
     */
    ScenicSpotProductMPO getProductById(String productId);

    /**
     * 根据景点id获取产品
     * @param spotId
     * @return
     */
    List<ScenicSpotProductMPO> getProductsBySpotId(String spotId);
}
