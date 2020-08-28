package com.huoli.trip.central.web.dao;

import com.huoli.trip.common.entity.PricePO;
import com.huoli.trip.common.entity.ProductItemPO;
import com.huoli.trip.common.entity.ProductPO;
import com.huoli.trip.common.vo.Coordinate;

import java.util.Date;
import java.util.HashMap;
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
     * 根据单个item查询
     * @param itemId
     * @return
     */
    List<ProductPO> getProductListByItemId(String itemId, Date saleDate);

    /**
     * 列表页
     * @param city
     * @param type
     * @param keyWord
     * @param page
     * @param size
     * @return
     */
    List<ProductPO> getPageListProduct(String city, Integer type, String keyWord, int page, int size);

    /**
     * 获取列表总数
     * @param city
     * @param type
     * @param keyWord
     * @return
     */
    int getPageListTotal(String city, Integer type, String keyWord);

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
     * @param productType
     * @param coordinate
     * @param radius
     * @param siz
     * @return
     */
    List<ProductPO> getNearRecommendResult(int productType, Coordinate coordinate, double radius, int siz);

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
     *
     * @param city
     * @param date
     * @param type
     * @return
     */
    List<ProductPO> getByCityAndType(String city, Date date, int type, int size);

    /**
     * item列表
     * @param city
     * @param type
     * @param keyWord
     * @param page
     * @param size
     * @return
     */
    List<ProductItemPO> getPageListForItem(String city, Integer type, String keyWord, int page, int size);

    /**
     * 查总数
     * @param city
     * @param type
     * @param keyWord
     * @return
     */
    long getPageListForItemTotal(String city, Integer type, String keyWord);

	/**
     *
     * @param city
     * @return
     */
    List<ProductPO> queryValidCity(String city);
    HashMap<String,String> queryValidCitys();

}
