package com.huoli.trip.central.web.dao.impl;

import com.huoli.trip.central.web.dao.ProductDao;
import com.huoli.trip.common.constant.Constants;
import com.huoli.trip.common.constant.ProductType;
import com.huoli.trip.common.entity.PricePO;
import com.huoli.trip.common.entity.ProductItemPO;
import com.huoli.trip.common.entity.ProductPO;
import com.huoli.trip.common.util.ListUtils;
import com.huoli.trip.common.vo.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.geo.Sphere;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2020/6/28<br>
 */
@Repository
public class ProductDaoImpl implements ProductDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    private GroupOperation getGroupField(){
        return Aggregation.group("mainItemCode")
                .first("mainItemCode").as("mainItemCode")
                .first("code").as("code")
                .first("supplierProductId").as("supplierProductId")
                .first("name").as("name")
                .first("supplierId").as("supplierId")
                .first("supplierName").as("supplierName")
                .first("mainItem").as("mainItem")
                .first("status").as("status")
                .first("productType").as("productType")
                .first("buyMax").as("buyMax")
                .first("buyMin").as("buyMin")
                .first("buyMinNight").as("buyMinNight")
                .first("buyMaxNight").as("buyMaxNight")
                .first("images").as("images")
                .first("bookAheadMin").as("bookAheadMin")
                .first("price").as("price")
                .first("salePrice").as("salePrice")
                .first("validTime").as("validTime")
                .first("invalidTime").as("invalidTime")
                .first("description").as("description")
                .first("excludeDesc").as("excludeDesc")
                .first("refundType").as("refundType")
                .first("delayType").as("delayType")
                .first("refundAheadMin").as("refundAheadMin")
                .first("refundDesc").as("refundDesc")
                .first("bookRules").as("bookRules")
                .first("allPreSale").as("allPreSale")
                .first("displayStart").as("displayStart")
                .first("displayEnd").as("displayEnd")
                .first("preSaleStart").as("preSaleStart")
                .first("preSaleEnd").as("preSaleEnd")
                .first("preSaleDescription").as("preSaleDescription")
                .first("limitRules").as("limitRules")
                .first("room").as("room")
                .first("ticket").as("ticket")
                .first("food").as("food")
                .first("city").as("city")
                .first("count").as("count");
    }

    @Override
    public List<ProductPO> getProductListByItemIds(List<String> itemIds){
        Query query = new Query(Criteria.where("mainItemCode").in(itemIds));
        return mongoTemplate.find(query, ProductPO.class);
    }
    @Override
    public List<ProductPO> getProductListByItemId(String itemId){
        Query query = new Query(Criteria.where("mainItemCode").is(itemId));
        return mongoTemplate.find(query, ProductPO.class);
    }

    @Override
    public List<ProductPO> getPageList(String city, Integer type, int page, int size){
        MatchOperation matchOperation = Aggregation.match(Criteria.where("productType").is(type).and("city").is(city));
        GroupOperation groupOperation = getGroupField();
//        Sort sort = Sort.by(Sort.Direction.ASC, "salePrice");
        long rows = (page - 1) * size;
        Aggregation aggregation = Aggregation.newAggregation(
                matchOperation,
                groupOperation.min("salePrice").as("salePrice"),
                Aggregation.project(ProductPO.class).andExclude("_id"),
                Aggregation.skip(rows),
                Aggregation.limit(size));
        AggregationResults<ProductPO> outputType = mongoTemplate.aggregate(aggregation, Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
        return outputType.getMappedResults();
    }

    @Override
    public int getListTotal(String city, Integer type){
        MatchOperation matchOperation = Aggregation.match(Criteria.where("productType").is(type).and("city").is(city));
        GroupOperation groupOperation = getGroupField();
        Aggregation aggregationCount = Aggregation.newAggregation(matchOperation,
                groupOperation.count().as("count"),
                Aggregation.project(ProductPO.class).andExclude("_id"));
        AggregationResults<ProductPO> resultsCount = mongoTemplate.aggregate(aggregationCount, Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
        return resultsCount.getMappedResults().size();
    }

    @Override
    public List<ProductPO> getCoordinateRecommendList(Coordinate coordinate, double radius, Integer type, int size){
        int itemType = getItemType(type);
        Point point = new Point(coordinate.getLongitude(), coordinate.getLatitude());
        Sphere sphere = new Sphere(point, new Distance(radius, Metrics.KILOMETERS));
        Query query = new Query(Criteria.where("itemType").is(itemType).and("itemCoordinate").within(sphere));
        List<ProductItemPO> productItemPOs = mongoTemplate.find(query, ProductItemPO.class);
        if(ListUtils.isEmpty(productItemPOs)){
            return null;
        }
        return getRecommendResult(productItemPOs.stream().map(item -> item.getCode()).collect(Collectors.toList()), size);
    }

    @Override
    public List<ProductPO> getCityRecommendList(String city, Integer type, int size){
        int itemType = getItemType(type);
        Query query = new Query(Criteria.where("itemType").is(itemType).and("city").is(city));
        List<ProductItemPO> productItemPOs = mongoTemplate.find(query, ProductItemPO.class);
        if(ListUtils.isEmpty(productItemPOs)){
            return null;
        }
        return getRecommendResult(productItemPOs.stream().map(item -> item.getCode()).collect(Collectors.toList()), size);
    }

    @Override
    public ProductPO getImagesByCode(String code){
        Query query = new Query(Criteria.where("code").is(code));
        query.fields().include("images");
        return mongoTemplate.findOne(query, ProductPO.class);
    }

    /**
     * 查推荐结果
     * @param itemCodes
     * @param size
     * @return
     */
    private List<ProductPO> getRecommendResult(List<String> itemCodes, int size){
        MatchOperation matchOperation = Aggregation.match(Criteria.where("mainItemCode").in(itemCodes));
        GroupOperation groupOperation = getGroupField();
        SortOperation sortOperation = Aggregation.sort(Sort.by(Sort.Direction.ASC, "salePrice"));
        Aggregation aggregation = Aggregation.newAggregation(
                matchOperation,
                groupOperation.min("salePrice").as("salePrice"),
                sortOperation,
                Aggregation.project(ProductPO.class).andExclude("_id"),
                Aggregation.limit(size));
        AggregationResults<ProductPO> outputType = mongoTemplate.aggregate(aggregation, Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
        return outputType.getMappedResults();
    }

    /**
     * 把product type转成productitem type
     * @param productType
     * @return
     */
    private int getItemType(int productType){
        int itemType;
        if(productType == ProductType.SCENIC_TICKET_PLUS.getCode() || productType == ProductType.SCENIC_TICKET.getCode()){
            itemType = Constants.PRODUCT_ITEM_TYPE_TICKET;
        } else if(productType == ProductType.RESTAURANT.getCode()){
            itemType = Constants.PRODUCT_ITEM_TYPE_FOOD;
        } else {
            itemType = Constants.PRODUCT_ITEM_TYPE_HOTEL;
        }
        return itemType;
    }

    @Override
    public PricePO getPricePos(String productCode) {
        Query query = new Query(Criteria.where("productCode").is(productCode));
        return mongoTemplate.findOne(query, PricePO.class);
    }

    @Override
    public ProductPO getTripProductByCode(String productCode) {
        Query query = new Query(Criteria.where("code").is(productCode));
        return mongoTemplate.findOne(query, ProductPO.class);
    }
}
