package com.huoli.trip.central.web.dao;

import com.huoli.trip.common.entity.ProductItemPO;

import java.util.List;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2020/6/28<br>
 */
public interface ProductItemDao {


    /**
     * 根据城市和类型查项目
     * @param city
     * @param type
     * @param pageSize
     * @return
     */
    List<ProductItemPO> getByCityAndType(String city, Integer type, int pageSize);

    /**
     * 根据code获取项目
     */
    ProductItemPO getByCode(String code);

    /**
     * 根据code获取图片
     * @param code
     * @return
     */
    ProductItemPO getImagesByCode(String code);
}
