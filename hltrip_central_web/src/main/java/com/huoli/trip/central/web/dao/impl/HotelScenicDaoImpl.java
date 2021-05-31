package com.huoli.trip.central.web.dao.impl;

import com.huoli.trip.central.web.dao.HotelScenicDao;
import com.huoli.trip.common.constant.MongoConst;
import com.huoli.trip.common.entity.mpo.hotel.HotelMPO;
import com.huoli.trip.common.entity.mpo.hotelScenicSpot.HotelScenicSpotProductBackupMPO;
import com.huoli.trip.common.entity.mpo.hotelScenicSpot.HotelScenicSpotProductMPO;
import com.huoli.trip.common.entity.mpo.hotelScenicSpot.HotelScenicSpotProductSetMealMPO;
import com.huoli.trip.common.vo.request.v2.CalendarRequest;
import com.huoli.trip.common.vo.request.v2.HotelScenicSetMealRequest;
import com.huoli.trip.common.vo.v2.HotelScenicProductDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @description:
 * @author: WangYing
 * @create: 2021-05-25 16:57
 **/
@Repository
@Slf4j
public class HotelScenicDaoImpl implements HotelScenicDao {
    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public HotelScenicProductDetail queryHotelScenicProductById(String productId) {
        Criteria criteria = new Criteria();
        criteria.and("_id").is(productId);
        MatchOperation matchOperation = Aggregation.match(criteria);
        ProjectionOperation projectionOperation = Aggregation.project(
                Fields.from(Fields.field("productId", "_id"),
                        Fields.field("productName"),
                        Fields.field("highlights"),
                        Fields.field("images"),
                        Fields.field("desc", "computerDesc"),
                        Fields.field("day"),
                        Fields.field("night"),
                        Fields.field("category")));
        Aggregation aggregation = Aggregation.newAggregation(matchOperation, projectionOperation);
        AggregationResults<HotelScenicProductDetail> output = mongoTemplate.aggregate(aggregation, MongoConst.COLLECTION_NAME_HOTEL_SCENICSPOT_PRODUCT, HotelScenicProductDetail.class);
        return CollectionUtils.isNotEmpty(output.getMappedResults()) ? output.getMappedResults().get(0) : null;
    }

    @Override
    public List<HotelScenicSpotProductSetMealMPO> queryHotelScenicSetMealList(CalendarRequest request) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.and("hotelScenicSpotProductId").is(request.getProductId());
        criteria.and("priceStocks").ne(null);
        if (StringUtils.isNotBlank(request.getSetMealId())) {
            criteria.and("_id").is(request.getSetMealId());
        }
        if(StringUtils.isNotBlank(request.getStartDate())){
            criteria.and("priceStocks.date").is(request.getStartDate());
        }
        query.addCriteria(criteria);
        return mongoTemplate.find(query, HotelScenicSpotProductSetMealMPO.class);
    }

    @Override
    public HotelScenicSpotProductMPO queryHotelScenicProductMpoById(String productId) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.and("_id").is(productId);
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, HotelScenicSpotProductMPO.class);
    }

    @Override
    public HotelScenicSpotProductSetMealMPO queryHotelScenicSetMealById(HotelScenicSetMealRequest request) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.and("_id").is(request.getPackageId());
        criteria.and("hotelScenicSpotProductId").is(request.getProductId());
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, HotelScenicSpotProductSetMealMPO.class);
    }

    @Override
    public HotelMPO queryHotelMpo(String hotelId) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.and("_id").is(hotelId);
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, HotelMPO.class);
    }

    @Override
    public HotelScenicSpotProductBackupMPO queryBackInfoByProductId(String productId) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.and("productMPO._id").is(productId);
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, HotelScenicSpotProductBackupMPO.class);
    }
}
