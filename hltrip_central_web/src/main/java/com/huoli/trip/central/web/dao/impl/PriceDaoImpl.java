package com.huoli.trip.central.web.dao.impl;

import com.huoli.trip.central.web.dao.PriceDao;
import com.huoli.trip.common.constant.Constants;
import com.huoli.trip.common.entity.PricePO;
import com.huoli.trip.common.entity.PriceSinglePO;
import com.huoli.trip.common.util.ListUtils;
import com.huoli.trip.common.util.MongoDateUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
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
public class PriceDaoImpl implements PriceDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void updateBySupplierProductId(PricePO pricePO){
        Query query = Query.query(Criteria.where("productCode").is(pricePO.getProductCode()));
        Document document = new Document();
        mongoTemplate.getConverter().write(pricePO, document);
        Update update = Update.fromDocument(document);
        mongoTemplate.upsert(query, update, Constants.COLLECTION_NAME_TRIP_PRICE_CALENDAR);
    }

    @Override
    public PriceSinglePO selectByProductCode(String productCode){
        Aggregation aggregation = Aggregation.newAggregation(Aggregation.unwind("priceInfos"),
                Aggregation.match(Criteria.where("productCode").is(productCode).and("priceInfos.stock").gt(0)),
                Aggregation.sort(Sort.Direction.ASC, "salePrice"),
                Aggregation.limit(1));
        AggregationResults<PriceSinglePO> output = mongoTemplate.aggregate(aggregation, Constants.COLLECTION_NAME_TRIP_PRICE_CALENDAR, PriceSinglePO.class);
        if(ListUtils.isNotEmpty(output.getMappedResults())){
            return output.getMappedResults().get(0);
        }
        return null;
    }

    @Override
    public PriceSinglePO selectByDate(String productCode, Date date){
        Aggregation aggregation = Aggregation.newAggregation(Aggregation.unwind("priceInfos"),
                Aggregation.match(Criteria.where("productCode").is(productCode).and("priceInfos.saleDate").is(MongoDateUtils.handleTimezoneInput(date))),
                Aggregation.limit(1));
        AggregationResults<PriceSinglePO> output = mongoTemplate.aggregate(aggregation, Constants.COLLECTION_NAME_TRIP_PRICE_CALENDAR, PriceSinglePO.class);
        if(ListUtils.isNotEmpty(output.getMappedResults())){
            return output.getMappedResults().get(0);
        }
        return null;
    }

    @Override
    public PriceSinglePO selectByProductCodes(List<String> productCodes, Date date){
        Aggregation aggregation = Aggregation.newAggregation(Aggregation.unwind("priceInfos"),
                Aggregation.match(Criteria.where("productCode").in(productCodes).and("priceInfos.saleDate").is(MongoDateUtils.handleTimezoneInput(date))),
                Aggregation.sort(Sort.Direction.ASC, "salePrice"),
                Aggregation.limit(1));
        AggregationResults<PriceSinglePO> output = mongoTemplate.aggregate(aggregation, Constants.COLLECTION_NAME_TRIP_PRICE_CALENDAR, PriceSinglePO.class);
        if(ListUtils.isNotEmpty(output.getMappedResults())){
            return output.getMappedResults().get(0);
        }
        return null;
    }
}
