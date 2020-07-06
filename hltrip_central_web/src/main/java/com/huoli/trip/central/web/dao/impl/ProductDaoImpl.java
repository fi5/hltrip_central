package com.huoli.trip.central.web.dao.impl;

import com.alibaba.fastjson.JSON;
import com.huoli.trip.central.web.dao.ProductDao;
import com.huoli.trip.common.constant.Constants;
import com.huoli.trip.common.entity.PricePO;
import com.huoli.trip.common.entity.ProductItemPO;
import com.huoli.trip.common.entity.ProductPO;
import com.huoli.trip.common.vo.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.geo.Sphere;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

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
@Repository
public class ProductDaoImpl implements ProductDao {

    @Autowired
    private MongoTemplate mongoTemplate;

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
        GroupOperation groupOperation = Aggregation.group("mainItemCode").first("mainItemCode").as("mainItemCode");
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
        GroupOperation groupOperation = Aggregation.group("mainItemCode").first("mainItemCode").as("mainItemCode");
        Aggregation aggregationCount = Aggregation.newAggregation(matchOperation,
                groupOperation.count().as("count"),
                Aggregation.project(ProductPO.class).andExclude("_id"));
        AggregationResults<ProductPO> resultsCount = mongoTemplate.aggregate(aggregationCount, Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
        return resultsCount.getMappedResults().size();
    }

    @Override
    public List<ProductPO> getCoordinateRecommendList(Coordinate coordinate, Integer type, int page, int size){
//        Point point = new Point(coordinate.getLongitude(), coordinate.getLatitude());
//        Sphere sphere = new Sphere(point, new Distance(100, Metrics.KILOMETERS));
//        Query query = new Query(Criteria.where("itemCoordinate").within(sphere));
//        return mongoTemplate.find(query, ProductItemPO.class);

        Point point = new Point(coordinate.getLongitude(), coordinate.getLatitude());
        MatchOperation matchOperation = Aggregation.match(Criteria.where("productType").is(type).and("mainItem.itemCoordinate").nearSphere(point));
        GroupOperation groupOperation = Aggregation.group("mainItemCode").first("mainItemCode").as("mainItemCode");
        SortOperation sortOperation = Aggregation.sort(Sort.by(Sort.Direction.ASC, "salePrice"));
        long rows = (page - 1) * size;
        Aggregation aggregation = Aggregation.newAggregation(
                matchOperation,
                groupOperation.min("salePrice").as("salePrice"),
                sortOperation,
                Aggregation.project(ProductPO.class).andExclude("_id"),
                Aggregation.skip(rows),
                Aggregation.limit(size));
        AggregationResults<ProductPO> outputType = mongoTemplate.aggregate(aggregation, Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
        return outputType.getMappedResults();
    }

    @Override
    public List<ProductPO> getCityRecommendList(String city, Integer type, Date date, int page, int size){
        MatchOperation matchOperation = Aggregation.match(Criteria.where("productType").is(type).and("city").is(city));
        GroupOperation groupOperation = Aggregation.group("mainItemCode").first("mainItemCode").as("mainItemCode");
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
    public PricePO getPricePos(String productCode) {
        Query query = new Query(Criteria.where("productCode").is(productCode));
        return mongoTemplate.findOne(query, PricePO.class);
    }

    @Override
    public List<ProductItemPO> getProductListByItemIdsPage(List<String> itemIds, int page, int size){
//        NearQuery nearQuery = NearQuery.near(116.481533, 39.996504, Metrics.KILOMETERS).maxDistance(new Distance(100000, Metrics.KILOMETERS));
//        System.out.println("打印。。" + JSON.toJSONString(mongoTemplate.geoNear(nearQuery, ProductItemPO.class).getContent()));
//        return null;
        Point point = new Point(116.481533, 39.996504);
        Sphere sphere = new Sphere(point, new Distance(100, Metrics.KILOMETERS));
        Query query = new Query(Criteria.where("itemCoordinate").within(sphere));
        return mongoTemplate.find(query, ProductItemPO.class);
//
//        Criteria criteria = Criteria.where("itype").in(itemIds);
////        Sort sort = Sort.by(Sort.Direction.ASC, "salePrice");
//        GroupOperation groupOperation = Aggregation.group("mainItemId").first("mainItemId").as("mainItemId")
//                .first("salePrice").as("salePrice")
//                .last("itype").as("itype");
//        long rows = (page - 1) * size;
//        Aggregation aggregation1 = Aggregation.newAggregation(Aggregation.match(criteria),
//                groupOperation.count().as("total"),
//                Aggregation.project(ProductPO.class).andExclude("_id"));
//        AggregationResults<ProductPO> outputType1 = mongoTemplate.aggregate(aggregation1, Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
//        System.out.println("total============ " + outputType1.getMappedResults().size());
//        System.out.println(JSON.toJSONString(outputType1.getMappedResults()));
//        SortOperation sortOperation = Aggregation.sort(Sort.by(Sort.Direction.ASC, "salePrice"));
//        Aggregation aggregation = Aggregation.newAggregation(
//                Aggregation.match(criteria),
//                groupOperation.min("salePrice").as("salePrice"),
//                sortOperation,
//                Aggregation.project(ProductPO.class).andExclude("_id"),
//                Aggregation.skip(rows),
//                Aggregation.limit(size));
//        AggregationResults<ProductPO> outputType = mongoTemplate.aggregate(aggregation, Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
//        return outputType.getMappedResults();
    }
}
