package com.huoli.trip.central.web.dao.impl;

import com.google.common.collect.Lists;
import com.huoli.trip.central.web.converter.ProductConverter;
import com.huoli.trip.central.web.dao.ProductDao;
import com.huoli.trip.common.constant.Constants;
import com.huoli.trip.common.entity.CityPO;
import com.huoli.trip.common.entity.PricePO;
import com.huoli.trip.common.entity.ProductPO;
import com.huoli.trip.common.util.DateTimeUtil;
import com.huoli.trip.common.util.ListUtils;
import com.huoli.trip.common.util.MongoDateUtils;
import com.huoli.trip.common.vo.Coordinate;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.MongoCursor;
import org.apache.commons.lang3.StringUtils;
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

import java.util.ArrayList;
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
@Repository
public class ProductDaoImpl implements ProductDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<ProductPO> getProductListByItemId(String itemId, Date saleDate){
        // 连价格日历表
        LookupOperation priceLookup = LookupOperation.newLookup().from(Constants.COLLECTION_NAME_TRIP_PRICE_CALENDAR)
                .localField("code")
                .foreignField("productCode")
                .as("priceCalendar");
        // 拆价格日历
        UnwindOperation unwindOperation = Aggregation.unwind("priceCalendar");
        UnwindOperation unwindOperation1 = Aggregation.unwind("priceCalendar.priceInfos");
        // 按价格正序
        SortOperation priceSort = Aggregation.sort(Sort.Direction.ASC, "priceCalendar.priceInfos.salePrice");
        // 查询条件
        Criteria criteria = Criteria.where("mainItemCode").is(itemId)
                .and("status").is(1)
                .and("priceCalendar.priceInfos.saleDate").is(MongoDateUtils.handleTimezoneInput(saleDate))
                .and("priceCalendar.priceInfos.stock").gt(0);
        MatchOperation matchOperation = Aggregation.match(criteria);
        // 指定字段
        ProjectionOperation projectionOperation = Aggregation.project(ProductPO.class).andExclude("_id");
        // 分组后排序
        List<AggregationOperation> aggregations = Lists.newArrayList(priceLookup,
                unwindOperation,
                unwindOperation1,
                matchOperation,
                priceSort,
                projectionOperation);
        Aggregation aggregation = Aggregation.newAggregation(aggregations);
        AggregationResults<ProductPO> output = mongoTemplate.aggregate(aggregation, Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
        return output.getMappedResults();
    }

    @Override
    public List<ProductPO> getPageList(String city, Integer type, String keyWord, int page, int size){
        List<AggregationOperation> aggregations = pageListAggregation(city, type, keyWord);
        long rows = (page - 1) * size;
        aggregations.add(Aggregation.skip(rows));
        aggregations.add(Aggregation.limit(size));
        Aggregation aggregation = Aggregation.newAggregation(aggregations);
        AggregationResults<ProductPO> outputType = mongoTemplate.aggregate(aggregation, Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
        return outputType.getMappedResults();
    }

    @Override
    public int getPageListTotal(String city, Integer type, String keyWord){
        List<AggregationOperation> aggregations = pageListAggregation(city, type, keyWord);
        CountOperation countOperation = Aggregation.count().as("count");
        aggregations.add(countOperation);
        Aggregation aggregation = Aggregation.newAggregation(aggregations);
        AggregationResults<ProductPO> outputType = mongoTemplate.aggregate(aggregation, Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
        if(ListUtils.isEmpty(outputType.getMappedResults())|| outputType.getMappedResults().get(0) == null){
            return 0;
        }
        return outputType.getMappedResults().get(0).getCount();
    }

    @Override
    public List<ProductPO> getSalesRecommendList(List<String> productCodes){
        // 查询条件
        Criteria criteria = Criteria.where("priceCalendar.priceInfos.stock").gt(0).and("status").is(0).and("code").in(productCodes);
        MatchOperation matchOperation = Aggregation.match(criteria);
        Aggregation aggregation = Aggregation.newAggregation(recommendListAggregation(matchOperation, 0));
        AggregationResults<ProductPO> output = mongoTemplate.aggregate(aggregation, Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
        return output.getMappedResults();
    }

    @Override
    public List<ProductPO> getFlagRecommendResult(Integer type, int size){
        // 查询条件
        Criteria criteria = Criteria.where("priceCalendar.priceInfos.stock").gt(0).and("status").is(1).and("recommendFlag").is(1);
        if(type != null){
            criteria.and("productType").is(type);
        }
        MatchOperation matchOperation = Aggregation.match(criteria);
        Aggregation aggregation = Aggregation.newAggregation(recommendListAggregation(matchOperation, size));
        AggregationResults<ProductPO> output = mongoTemplate.aggregate(aggregation, Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
        return output.getMappedResults();
    }

    @Override
    public List<ProductPO> getNearRecommendResult(int productType, Coordinate coordinate, double radius, int size){
        // 获取item类型
        int itemType = ProductConverter.getItemType(productType);
        // 附近
        Point point = new Point(coordinate.getLongitude(), coordinate.getLatitude());
        Sphere sphere = new Sphere(point, new Distance(radius, Metrics.KILOMETERS));
        // 查询条件
        Criteria criteria = Criteria.where("mainItem.itemType").is(itemType)
                .and("mainItem.itemCoordinate").within(sphere)
                .and("priceCalendar.priceInfos.stock").gt(0)
                .and("status").is(1);
        MatchOperation matchOperation = Aggregation.match(criteria);
        // 连item表
        List<AggregationOperation> operations = Lists.newArrayList(LookupOperation.newLookup().from(Constants.COLLECTION_NAME_TRIP_PRODUCT_ITEM)
                .localField("mainItemCode")
                .foreignField("code")
                .as("mainItem"));
        operations.addAll(recommendListAggregation(matchOperation, size));
        Aggregation aggregation = Aggregation.newAggregation(operations);
        AggregationResults<ProductPO> output = mongoTemplate.aggregate(aggregation, Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
        return output.getMappedResults();
    }

    @Override
    public List<ProductPO> getByCityAndType(String city, Date date, int type, int size){
        // 查询条件
        Criteria criteria = Criteria.where("priceCalendar.priceInfos.stock").gt(0)
                .and("status").is(1)
                .and("priceCalendar.priceInfos.saleDate").is(MongoDateUtils.handleTimezoneInput(date))
                .and("city").in(city).and("productType").is(type);
        MatchOperation matchOperation = Aggregation.match(criteria);
        Aggregation aggregation = Aggregation.newAggregation(recommendListAggregation(matchOperation, size));
        AggregationResults<ProductPO> output = mongoTemplate.aggregate(aggregation, Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
        return output.getMappedResults();
    }

    @Override
    public ProductPO getImagesByCode(String code){
        Query query = new Query(Criteria.where("code").is(code));
        query.fields().include("images");
        return mongoTemplate.findOne(query, ProductPO.class);
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

    /**
     * 构建分组条件
     * @param fields
     * @return
     */
    private GroupOperation getGroupField(String... fields){
        return Aggregation.group(fields)
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
                .first("count").as("count")
                .first("priceCalendar").as("priceCalendar");
    }

    /**
     * 构建列表页条件
     * @param city
     * @param type
     * @param keyWord
     * @return
     */
    private List<AggregationOperation> pageListAggregation(String city, Integer type, String keyWord){
        // 查询条件
        Criteria criteria = Criteria.where("priceCalendar.priceInfos.stock").gt(0)
                .and("status").is(1)
                .and("priceCalendar.priceInfos.saleDate").gte(MongoDateUtils.handleTimezoneInput(DateTimeUtil.trancateToDate(new Date())))
                .and("productType").is(type)
                .and("city").is(city);
        if(StringUtils.isNotBlank(keyWord)){
            criteria.orOperator(Criteria.where("city").regex(keyWord), Criteria.where("name").regex(keyWord));
        }
        MatchOperation matchOperation = Aggregation.match(criteria);
        // 分组
        GroupOperation groupOperation = getGroupField("mainItemCode");
        return ListAggregation(matchOperation, groupOperation);
    }

    /**
     * 构建推荐列表条件
     * @param matchOperation
     * @param size
     * @return
     */
    private  List<AggregationOperation> recommendListAggregation(MatchOperation matchOperation, int size){
        // 分组
        GroupOperation groupOperation = getGroupField("code");
        List<AggregationOperation> operations = ListAggregation(matchOperation, groupOperation);
        if(size > 0){
            operations.add(Aggregation.limit(size));
        }
        return operations;
    }

    /**
     * 构建通用条件
     * @param matchOperation
     * @param groupOperation
     * @return
     */
    private List<AggregationOperation> ListAggregation(MatchOperation matchOperation, GroupOperation groupOperation){
        // 连价格日历表
        LookupOperation priceLookup = LookupOperation.newLookup().from(Constants.COLLECTION_NAME_TRIP_PRICE_CALENDAR)
                .localField("code")
                .foreignField("productCode")
                .as("priceCalendar");
        // 拆价格日历
        UnwindOperation unwindOperation = Aggregation.unwind("priceCalendar");
        UnwindOperation unwindOperation1 = Aggregation.unwind("priceCalendar.priceInfos");
        // 按价格正序
        SortOperation priceSort = Aggregation.sort(Sort.Direction.ASC, "priceCalendar.priceInfos.salePrice");
        // 指定字段
        ProjectionOperation projectionOperation = Aggregation.project(ProductPO.class).andExclude("_id");
        // 分组后排序
        SortOperation sortOperation = Aggregation.sort(Sort.by(Sort.Direction.ASC, "salePrice"));
        return Lists.newArrayList(priceLookup,
                unwindOperation,
                unwindOperation1,
                matchOperation,
                priceSort,
                groupOperation.min("priceCalendar.priceInfos.salePrice").as("salePrice"),
                sortOperation,
                projectionOperation);
    }


    public List<ProductPO> queryValidCity(String city){
        // 查询条件
        Query query = new Query(Criteria.where("city").in(city));
        List<ProductPO> pros = mongoTemplate.find(query, ProductPO.class);
        return pros;
    }

    public HashMap<String,String> queryValidCitys(){
        // 查询条件
//        Query query = new Query(Criteria.where("status").is(1));
        final DistinctIterable<String> citys = mongoTemplate.getCollection(Constants.COLLECTION_NAME_TRIP_PRODUCT).distinct("city", String.class);
        final MongoCursor<String> iterator = citys.iterator();
        HashMap<String,String> map=new HashMap<>();
        while (iterator.hasNext()){
            map.put(iterator.next(),Constants.COLLECTION_NAME_TRIP_CITY);
        }
        return map;
    }
}
