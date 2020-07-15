package com.huoli.trip.central.web.dao;

import com.huoli.trip.common.entity.PricePO;
import com.huoli.trip.common.entity.ProductItemPO;
import com.huoli.trip.common.entity.ProductPO;
import com.huoli.trip.common.vo.Coordinate;

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
public interface ProductDao {

    /**
     * 根据多个item查产品
     * @param itemIds
     * @return
     */
    List<ProductPO> getProductListByItemIds(List<String> itemIds);

    /**
     * 根据单个item查询
     * @param itemId
     * @return
     */
    List<ProductPO> getProductListByItemId(String itemId);

    /**
     * 列表页
     * @param city
     * @param type
     * @param keyWord
     * @param page
     * @param size
     * @return
     */
    List<ProductPO> getPageList(String city, Integer type, String keyWord, int page, int size);

    /**
     * 总数
     * @param city
     * @param type
     * @return
     */
    int getListTotal(String city, Integer type);

    /**
     * 按销量获取推荐列表
     * @param productCodes
     * @return
     */
    List<ProductPO> getSalesRecommendList(List<String> productCodes);

    /**
     * 按推荐标识获取推荐列表
     * @param type
     * @param size
     * @return
     */
    List<ProductPO> getFlagRecommendResult(Integer type, int size);

    /**
     * 获取低价推荐列表
     * @param itemCodes
     * @param size
     * @return
     */
    List<ProductPO> getLowPriceRecommendResult(List<String> itemCodes, int size);

    /**
     * 获取图片列表
     * @param code
     * @return
     */
    ProductPO getImagesByCode(String code);

	/**
     * 获取价格日历
     * @param productCode
     * @return
     */
    PricePO getPricePos(String productCode);

	/**
     * 获取product
     * @param productCode
     * @return
     */
    ProductPO getTripProductByCode(String productCode);

    /**
     * 根据城市和类型获取产品
     * @param city
     * @param type
     * @return
     */
    List<ProductPO> getByCityAndType(String city, int type);

}
