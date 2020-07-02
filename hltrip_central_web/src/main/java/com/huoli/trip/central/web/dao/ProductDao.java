package com.huoli.trip.central.web.dao;

import com.huoli.trip.common.entity.ProductPO;

import java.util.List;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2020/6/28<br>
 */
public interface ProductDao {

    /**
     * 根据item查产品
     * @param itemIds
     * @return
     */
    List<ProductPO> getProductListByItemIds(List<String> itemIds);

    List<ProductPO> getPageList(String city, Integer type, int page, int size);
    List<ProductPO> getProductListByItemIdsPage(List<String> itemIds, int page, int size);

}
