package com.huoli.trip.central.web.dao.impl;


import com.alibaba.fastjson.JSONObject;
import com.huoli.trip.central.web.dao.ScenicSpotDao;
import com.huoli.trip.central.web.util.CentralUtils;
import com.huoli.trip.common.entity.mpo.ProductListMPO;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.Sphere;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.geo.Point;

import java.util.List;

/**
 * @time   :2021/3/19
 * @comment:
 **/
@Slf4j
@Repository
public class ScenicSpotDaoImpl implements ScenicSpotDao {

    @Autowired
    private MongoTemplate mongoTemplate;


    @Override
    public ScenicSpotMPO qyerySpotById(String scenicSpotId) {
        Query query = new Query(Criteria.where("_id").is(scenicSpotId));
        //query.fields().include("_id").include("name").include("address").include("level").include("operatingStatus").include("coordinate").include("scenicSpotOpenTimes").include("otherOpenTimeDesc").include("briefDesc");
        ScenicSpotMPO spot = mongoTemplate.findOne(query, ScenicSpotMPO.class);
        return spot;
    }

    @Override
    public List<ProductListMPO> querySpotProductBySpotIdAndDate(String scenicSpotId, String date) {

        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.and("scenicSpotId").is(scenicSpotId);
        criteria.and("status").is(1);
        criteria.and("isDel").is(0);
        criteria.and("type").is(0);
        query.fields().include("_id").include("productId").include("productName").include("scenicSpotId");
        mongoTemplate.find(query,ProductListMPO.class);
        return null;
    }

    @Override
    public List<ScenicSpotProductMPO> querySpotProduct(String scenicSpotId, List<String> channelInfo) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.and("scenicSpotId").is(scenicSpotId);
        criteria.and("status").is(1);
        if (CollectionUtils.isNotEmpty(channelInfo)) {
            criteria.and("channel").in(channelInfo);
        }
        query.addCriteria(criteria);
        List<ScenicSpotProductMPO> scenicSpotProductMPOS = mongoTemplate.find(query, ScenicSpotProductMPO.class);
        return scenicSpotProductMPOS;
    }

    @Override
    public List<ScenicSpotProductPriceMPO> queryProductPriceByProductId(String scenicSpotProductId) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.and("scenicSpotProductId").is(scenicSpotProductId);
        criteria.and("stock").gt(0);
        query.addCriteria(criteria);
        return  mongoTemplate.find(query, ScenicSpotProductPriceMPO.class);
    }

    @Override
    public List<ScenicSpotProductPriceMPO> queryPriceByProductIdAndDate(String scenicSpotProductId, String startDate, String endDate) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.and("scenicSpotProductId").is(scenicSpotProductId);
        criteria.and("stock").gt(0);
        query.addCriteria(criteria);
        if(StringUtils.isNotEmpty(startDate) && StringUtils.isNotEmpty(endDate)){
            criteria.andOperator(
                    Criteria.where("startDate").gte(startDate),
                    Criteria.where("endDate").lte(endDate));

        }else {
            if (StringUtils.isNotEmpty(startDate)) {
                criteria.and("startDate").gte(startDate);
            }
            if (StringUtils.isNotEmpty(endDate)) {
                criteria.and("endDate").lte(endDate);
            }
        }
        return  mongoTemplate.find(query, ScenicSpotProductPriceMPO.class);
    }

    @Override
    public List<ScenicSpotProductPriceMPO> queryPrice(String scenicSpotProductId, String startDate, String endDate, String ruleId, String ticketKind) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.and("scenicSpotProductId").is(scenicSpotProductId);
        criteria.and("stock").gt(0);
        if(StringUtils.isNotEmpty(startDate) && StringUtils.isNotEmpty(endDate)){
            criteria.andOperator(
                    Criteria.where("startDate").gte(startDate),
                    Criteria.where("endDate").lte(endDate));

        }else {
            if (StringUtils.isNotEmpty(startDate)) {
                criteria.and("startDate").gte(startDate);
            }
            if (StringUtils.isNotEmpty(endDate)) {
                criteria.and("endDate").lte(endDate);
            }
        }
        if(StringUtils.isNotBlank(ruleId)){
            criteria.and("scenicSpotRuleId").is(ruleId);
        }
        if(StringUtils.isNotBlank(ruleId)){
            criteria.and("ticketKind").is(ticketKind);
        }
        query.addCriteria(criteria);
        return  mongoTemplate.find(query, ScenicSpotProductPriceMPO.class);
    }

    @Override
    public List<ScenicSpotProductPriceMPO> queryPriceByProductIds(List<String> productIds, String startDate, String endDate) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.and("scenicSpotProductId").in(productIds);
        criteria.and("stock").gt(0);
        query.addCriteria(criteria);
        if(StringUtils.isNotEmpty(startDate) && StringUtils.isNotEmpty(endDate)){
            criteria.andOperator(
                    Criteria.where("startDate").gte(startDate),
                    Criteria.where("endDate").lte(endDate));

        }else {
            if (StringUtils.isNotEmpty(startDate)) {
                criteria.and("startDate").gte(startDate);
            }
            if (StringUtils.isNotEmpty(endDate)) {
                criteria.and("endDate").lte(endDate);
            }
        }
        return  mongoTemplate.find(query, ScenicSpotProductPriceMPO.class);
    }
    @Override
    public List<ScenicSpotMPO> queryScenicSpotByPoint(double longitude,double latitude) {
        Query query = new Query();
        Point point = new Point(longitude,latitude);
        Criteria criteria = Criteria.where("coordinate").near(point).maxDistance( 30 / 111.12);
        query.addCriteria(criteria);
        return  mongoTemplate.find(query, ScenicSpotMPO.class);
    }
    @Override
    public ScenicSpotRuleMPO queryRuleById(String ruleId) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.and("_id").is(ruleId);
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, ScenicSpotRuleMPO.class);
    }

    @Override
    public ScenicSpotProductMPO querySpotProductById(String productId, List<String> channelInfo) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.and("_id").is(productId);
        if (CollectionUtils.isNotEmpty(channelInfo)) {
            criteria.and("channel").in(channelInfo);
        }
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query,ScenicSpotProductMPO.class);
    }

    @Override
    public ScenicSpotProductPriceMPO querySpotProductPriceById(String priceId) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.and("_id").is(priceId);
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query,ScenicSpotProductPriceMPO.class);
    }

    @Override
    public ScenicSpotProductBackupMPO queryBackInfoByProductId(String productId) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.and("scenicSpotProduct.id").is(productId);
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, ScenicSpotProductBackupMPO.class);
    }

    @Override
    public List<ScenicSpotMPO> queryByKeyword(String keyword, Integer count, String city, String cityCode) {
        Criteria criteria = new Criteria();
        criteria.and("del").is(0);
        criteria.and("name").regex(keyword);
        if (StringUtils.isNotEmpty(city) && StringUtils.isNotEmpty(cityCode)) {
            criteria.and("city").is(city).and("cityCode").is(cityCode);
        }
        Query query = new Query(criteria);
        query.fields().include("_id").include("name").include("city").include("cityCode");
        if (count != null) {
            query.limit(count);
        }
        log.info("criteria:{}", JSONObject.toJSONString(criteria));
        return mongoTemplate.find(query, ScenicSpotMPO.class);
    }


}
