package com.huoli.trip.central.web.dao;

import com.huoli.trip.common.entity.PricePO;
import com.huoli.trip.common.entity.PriceSinglePO;

import java.util.Date;
import java.util.List;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2020/6/28<br>
 */
public interface PriceDao {

    /**
     * 同步价格
     * @param pricePO
     */
    void updateBySupplierProductId(PricePO pricePO);

    /**
     * 根据产品编码获取最近的价格
     * @param productCode
     * @return
     */
    PriceSinglePO selectByProductCode(String productCode);

    /**
     * 根据产品编码和日期查价格
     * @param productCode
     * @param date
     * @return
     */
    PriceSinglePO selectByDate(String productCode, Date date);

    /**
     * 从多个产品获取最低价
     * @param productCodes
     * @param date
     * @return
     */
    PriceSinglePO selectByProductCodes(List<String> productCodes, Date date);

    /**
     * 根据产品编码查价格
     * @param productCode
     * @param count
     * @return
     */
    List<PriceSinglePO> selectByProductCode(String productCode, int count);

    /**
     * 查产品价格
     * @param productCode
     * @return
     */
    PricePO selectPricePOByProductCode(String productCode);
}
