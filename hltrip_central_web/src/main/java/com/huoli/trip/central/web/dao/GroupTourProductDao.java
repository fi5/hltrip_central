package com.huoli.trip.central.web.dao;

import com.huoli.trip.common.entity.mpo.groupTour.GroupTourProductMPO;

import java.util.List;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2021/6/1<br>
 */
public interface GroupTourProductDao {

    /**
     * 根据id获取产品
     * @param productId
     * @return
     */
    GroupTourProductMPO getProductById(String productId);

    /**
     * 根据名字获取产品
     * @param name
     * @return
     */
    List<GroupTourProductMPO> getProductsByName(String name);
}
