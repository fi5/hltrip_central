package com.huoli.trip.central.web.dao.impl;

import com.huoli.trip.central.web.dao.ProductDao;
import com.huoli.trip.common.constant.Constants;
import com.huoli.trip.common.entity.ProductPO;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

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
        Query query = new Query(Criteria.where("mainItemId").in(itemIds));
        return mongoTemplate.find(query, ProductPO.class);
    }

    @Override
    public List<ProductPO> getPageList(String city, Integer type, int page, int size){
        Criteria criteria = Criteria.where("productType").is(type).and("city").is(city);
//        Sort sort = Sort.by(Sort.Direction.ASC, "salePrice");
        long rows = (page - 1) * size;
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group("mainItemId").first("mainItemId").as("mainItemId").min("salePrice").as("salePrice"),
                Aggregation.count().as("count"),
                Aggregation.project(ProductPO.class).andExclude("_id"),
                Aggregation.skip(rows),
                Aggregation.limit(size));
        AggregationResults<ProductPO> outputType = mongoTemplate.aggregate(aggregation, Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
        return outputType.getMappedResults();
    }

    @Override
    public List<ProductPO> getProductListByItemIdsPage(List<String> itemIds, int page, int size){
        Criteria criteria = Criteria.where("itype").in(itemIds);
//        Sort sort = Sort.by(Sort.Direction.ASC, "salePrice");
        long rows = (page - 1) * size;
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group("mainItemId").first("mainItemId").as("mainItemId").min("salePrice").as("salePrice"),
                Aggregation.project(ProductPO.class).andExclude("_id"),
                Aggregation.skip(rows),
                Aggregation.limit(size));
        AggregationResults<ProductPO> outputType = mongoTemplate.aggregate(aggregation, Constants.COLLECTION_NAME_TRIP_PRODUCT, ProductPO.class);
        return outputType.getMappedResults();
    }
}
