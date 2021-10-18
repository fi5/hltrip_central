package com.huoli.trip.central.web.dao;

import com.huoli.trip.common.entity.PricePO;
import com.huoli.trip.common.entity.ProductItemPO;
import com.huoli.trip.common.entity.ProductPO;
import com.huoli.trip.common.entity.RecommendProductPO;
import com.huoli.trip.common.entity.mpo.ProductListMPO;
import com.huoli.trip.common.vo.Coordinate;
import com.huoli.trip.common.vo.request.goods.GroupTourListReq;
import com.huoli.trip.common.vo.request.goods.HotelScenicListReq;
import com.huoli.trip.common.vo.request.goods.ScenicTicketListReq;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

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
    List<ProductPO> getProductListByItemId(String itemId, Date saleDate, String appFrom);

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

    List<ProductPO> getFlagRecommendResult_(Integer type, int size);

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
     * @param oriCity
     * @param desCity
     * @param type
     * @param keyWord
     * @param page
     * @param size
     * @return
     */
    List<ProductItemPO> getPageListForItem(String oriCity, String desCity,  Integer type, String keyWord, String appFrom, int page, int size);

    /**
     * 查总数
     * @param oriCity
     * @param desCity
     * @param type
     * @param keyWord
     * @return
     */
    long getPageListForItemTotal(String oriCity, String desCity, Integer type, String keyWord, String appFrom);

    /**
     * 预览
     * @param productCode
     * @return
     */
    ProductPO getPreviewDetail(String productCode);

    /**
     * 设置展示状态
     * @param ids
     * @param display
     */
    void updateRecommendDisplay(List<String> ids, int display, int position);

    /**
     * 设置不展示
     * @param ids 排除的id
     */
    void updateRecommendNotDisplay(List<String> ids, int position);

    /**
     * 推荐产品
     * @return
     */
    List<RecommendProductPO> getRecommendProducts();

    /**
     * 更新推荐状态
     * @param productCode
     */
    void updateRecommendProductStatus(String productCode, Integer productStatus);

	/**
     *
     * @param city
     * @return
     */
    List<ProductPO> queryValidCity(String city);
    HashMap<String,String> queryValidCitys();

    List<ProductListMPO> scenicTickets(ScenicTicketListReq req, List<String> channelInfo, boolean isfullMatchCit);

    List<ProductListMPO> groupTourList(GroupTourListReq req, List<String> channelInfo);

    List<ProductListMPO> hotelScenicList(HotelScenicListReq req, List<String> channelInfo);

    int getScenicTicketTotal(ScenicTicketListReq req, List<String> channelInfo, boolean isFullMatchCity);

    int groupTourListCount(GroupTourListReq req, List<String> channelInfo);

    int hotelScenicListCount(HotelScenicListReq req, List<String> channelInfo);

    Set<String> getAllCity();
}
