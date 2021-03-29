package com.huoli.trip.central.web.dao.impl;

import com.google.common.collect.Lists;
import com.huoli.trip.central.web.converter.ProductConverter;
import com.huoli.trip.central.web.dao.ProductDao;
import com.huoli.trip.common.constant.Constants;
import com.huoli.trip.common.entity.*;
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
import org.springframework.data.mongodb.core.query.Update;
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
    public List<ProductPO> getProductListByItemId(String itemId, Date saleDate, String appFrom){
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
                // 兼容中石化
//                .and("priceCalendar.priceInfos.saleDate").is(MongoDateUtils.handleTimezoneInput(saleDate))
                .and("priceCalendar.priceInfos.stock").gt(0)
                .and("priceCalendar.priceInfos.salePrice").gt(0);
//        if(StringUtils.isNotBlank(appFrom)){
//            criteria.and("appFrom").in(appFrom);
//        }
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
    public ProductPO getPreviewDetail(String productCode){
        ProductPO productPO = mongoTemplate.findOne(Query.query(Criteria.where("code").is(productCode)), ProductPO.class);
        return productPO;
    }

    @Override
    public List<ProductPO> getPageListProduct(String city, Integer type, String keyWord, int page, int size){
        List<AggregationOperation> aggregations = pageListAggregation(city, type, keyWord);
        long rows = (page - 1) * size;
        aggregations.add(Aggregation.skip(rows));
        aggregations.add(Aggregation.limit(size));
        Aggregation aggregation = Aggregation.newAggregation(aggregations);
        AggregationResults<ProductPO> outputType = mongoTemplate.aggregate(aggregation.withOptions(AggregationOptions.builder().allowDiskUse(true).build()), Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
        return outputType.getMappedResults();
    }

    @Override
    public int getPageListTotal(String city, Integer type, String keyWord){
        List<AggregationOperation> aggregations = pageListAggregation(city, type, keyWord);
        CountOperation countOperation = Aggregation.count().as("count");
        aggregations.add(countOperation);
        Aggregation aggregation = Aggregation.newAggregation(aggregations);
        AggregationResults<ProductPO> outputType = mongoTemplate.aggregate(aggregation.withOptions(AggregationOptions.builder().allowDiskUse(true).build()), Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
        if(ListUtils.isEmpty(outputType.getMappedResults())|| outputType.getMappedResults().get(0) == null){
            return 0;
        }
        return outputType.getMappedResults().get(0).getCount();
    }

    @Override
    public List<ProductPO> getSalesRecommendList(List<String> productCodes){
        // 查询条件
        Criteria criteria = Criteria.where("code").in(productCodes).and("status").is(0).and("priceCalendar.priceInfos.stock").gt(0);
        MatchOperation matchOperation = Aggregation.match(criteria);
        Aggregation aggregation = Aggregation.newAggregation(recommendListAggregation(matchOperation, 0));
        AggregationResults<ProductPO> output = mongoTemplate.aggregate(aggregation, Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
        return output.getMappedResults();
    }

    @Override
    public List<ProductPO> getFlagRecommendResult_(Integer type, int size){
        // 查询条件
        Criteria criteria = Criteria.where("recommendFlag").is(1).and("status").is(1);
        if(type != null){
            criteria.and("productType").is(type);
        }
        MatchOperation matchOperation = Aggregation.match(criteria);
        Aggregation aggregation = Aggregation.newAggregation(recommendListAggregation(matchOperation, size));
        AggregationResults<ProductPO> output = mongoTemplate.aggregate(aggregation, Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
        return output.getMappedResults();
    }

    @Override
    public List<ProductPO> getFlagRecommendResult(Integer type, int size){
        // 查询条件
        Criteria criteria = Criteria.where("recommendFlag").is(1).and("status").is(1);
        if(type != null){
            criteria.and("productType").is(type);
        }
        Query query = new Query(criteria).limit(size);
        return mongoTemplate.find(query, ProductPO.class);
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

    @Override
    public List<ProductItemPO> getPageListForItem(String oriCity, String desCity, Integer type, String keyWord, String appFrom, int page, int size){
        long rows = (page - 1) * size;
        Query query = pageListForItemQuery(oriCity, desCity, type, keyWord, appFrom);
        query.skip(rows).limit(size);
        return mongoTemplate.find(query, ProductItemPO.class);
    }

    @Override
    public long getPageListForItemTotal(String oriCity, String desCity, Integer type, String keyWord, String appFrom){
        Query query = pageListForItemQuery(oriCity, desCity, type, keyWord, appFrom);
        return mongoTemplate.count(query, ProductItemPO.class);
    }

    @Override
    public void updateRecommendDisplay(List<String> ids, int display, int position){
        mongoTemplate.updateMulti(new Query(Criteria.where("position").is(position).and("_id").in(ids)),
                Update.update("display", display), RecommendProductPO.class);
    }

    @Override
    public void updateRecommendNotDisplay(List<String> ids, int position){
        mongoTemplate.updateMulti(new Query(Criteria.where("position").is(position).and("_id").nin(ids)),
                Update.update("display", Constants.RECOMMEND_DISPLAY_NO), RecommendProductPO.class);
    }

    @Override
    public List<RecommendProductPO> getRecommendProducts(){
        return mongoTemplate.find(
                new Query(Criteria.where("status").is(Constants.RECOMMEND_STATUS_VALID)
                .and("productStatus").is(Constants.PRODUCT_STATUS_VALID)), RecommendProductPO.class);
    }

    @Override
    public void updateRecommendProductStatus(String productCode, Integer productStatus){
        mongoTemplate.updateFirst(new Query(Criteria.where("productCode").is(productCode)),
                Update.update("status", Constants.RECOMMEND_STATUS_INVALID)
                .set("productStatus", productStatus),
                RecommendProductPO.class);
    }

    private Query pageListForItemQuery(String oriCity, String desCity, Integer type, String keyWord, String appFrom){
        Criteria criteria = new Criteria();
        Criteria criteriaOriCity = new Criteria();
        Criteria criteriaKeyWord = new Criteria();
        Date date = new Date();
        criteria.and("product.productType").is(type)
                .and("product.status").is(Constants.PRODUCT_STATUS_VALID)
                .and("product.supplierStatus").is(Constants.SUPPLIER_STATUS_OPEN)
                .and("product.auditStatus").is(Constants.VERIFY_STATUS_PASSING)
                .and("product.validTime").lte(MongoDateUtils.handleTimezoneInput(DateTimeUtil.trancateToDate(date)))
                .and("product.invalidTime").gte(MongoDateUtils.handleTimezoneInput(DateTimeUtil.trancateToDate(date)))
                .and("status").is(1)
                .and("auditStatus").is(1);
        if(StringUtils.isNotBlank(appFrom)){
            criteria.and("product.appFrom").in(appFrom);
        }
        if(StringUtils.isNotBlank(desCity)){
            criteria.and("city").regex(desCity);
        }
        // 只有旅游产品会查出发地
        if(Constants.TRIP_PRODUCT_LIST.contains(type)){
            if(StringUtils.isNotBlank(oriCity)){
                criteriaOriCity.orOperator(Criteria.where("oriCity").regex(oriCity), Criteria.where("oriCity").regex("全国"));
            }
        }
        if(StringUtils.isNotBlank(keyWord)){
            criteriaKeyWord.orOperator(Criteria.where("city").regex(keyWord), Criteria.where("name").regex(keyWord));
        }
        criteria.andOperator(criteriaOriCity, criteriaKeyWord);
        Query query = new Query(criteria);
        query.fields().include("code")
                .include("name")
                .include("tags")
                .include("description")
                .include("oriCity")
                .include("city")
                .include("mainImages")
                .include("images")
                .include("supplierId")
                .include("product.code")
                .include("product.name")
                .include("product.status")
                .include("product.productType")
                .include("product.images")
                .include("product.price")
                .include("product.salePrice")
                .include("product.description")
                .include("product.city")
                .include("product.count")
                .include("product.priceCalendar")
                .include("product.validTime")
                .include("product.invalidTime")
                .include("product.supplierId");
        return query;
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

    private GroupOperation getListGroupField(String... fields){
        return Aggregation.group(fields)
                .first("mainItemCode").as("mainItemCode")
                .first("code").as("code")
                .first("name").as("name")
                .first("mainItem").as("mainItem")
                .first("status").as("status")
                .first("productType").as("productType")
                .first("images").as("images")
                .first("price").as("price")
                .first("salePrice").as("salePrice")
                .first("description").as("description")
                .first("city").as("city")
                .first("count").as("count")
                .first("priceCalendar").as("priceCalendar");
    }

    private Fields getListFields(){
        return Fields.from(Fields.field("mainItemCode"),
                Fields.field("code"),
                Fields.field("name"),
                Fields.field("mainItem"),
                Fields.field("status"),
                Fields.field("productType"),
                Fields.field("images"),
                Fields.field("price"),
                Fields.field("salePrice"),
                Fields.field("description"),
                Fields.field("city"),
                Fields.field("count"),
                Fields.field("priceCalendar"));
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
        Criteria criteria = Criteria.where("city").is(city)
                .and("productType").is(type)
                .and("status").is(1);
        if(StringUtils.isNotBlank(keyWord)){
            criteria.orOperator(Criteria.where("city").regex(keyWord), Criteria.where("name").regex(keyWord));
        }
        MatchOperation matchOperation = Aggregation.match(criteria);
        // 分组
        GroupOperation groupOperation = getListGroupField("mainItemCode");
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
        GroupOperation groupOperation = getListGroupField("mainItemCode");
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
        ProjectionOperation projectionOperation = Aggregation.project(getListFields()).andExclude("_id");
        // 拆分后的条件
        MatchOperation matchOperation1 = Aggregation.match(Criteria.where("priceCalendar.priceInfos.stock").gt(0)
                .and("priceCalendar.priceInfos.saleDate").gte(MongoDateUtils.handleTimezoneInput(DateTimeUtil.trancateToDate(new Date()))));
        // 分组后排序
        SortOperation sortOperation = Aggregation.sort(Sort.by(Sort.Direction.ASC, "salePrice"));
        return Lists.newArrayList(matchOperation,
                priceLookup,
                unwindOperation,
                unwindOperation1,
                matchOperation1,
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
