package com.huoli.trip.central.web.dao;

import com.huoli.trip.common.entity.SupplierPolicyPO;
import com.huoli.trip.common.vo.IncreasePrice;

import java.util.List;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2021/1/6<br>
 */
public interface SupplierPolicyDao {
    /**
     * 查配置
     * @param supplierId
     * @return
     */
    SupplierPolicyPO getSupplierPolicyBySupplierId(String supplierId);

    /**
     * 多条件查配置
     * @param increasePrice
     * @return
     */
    List<SupplierPolicyPO> getSupplierPolicy(IncreasePrice increasePrice);
}
