package com.huoli.trip.central.web.dao.impl;


import com.huoli.trip.central.web.dao.ScenicSpotDao;
import com.huoli.trip.common.entity.mpo.ProductListMPO;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    public ScenicSpotPayServiceMPO querySpotPayItem(String spotPayItemId) {
        return null;
    }

    @Override
    public List<ScenicSpotProductMPO> querySpotProduct(String scenicSpotId) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.and("scenicSpotId").is(scenicSpotId);
        criteria.and("status").is(1);
        query.addCriteria(criteria);

        query.fields().include("_id").include("productId").include("productName").include("price").include("type").include("tags").include("scenicSpotOpenTimes").include("otherOpenTimeDesc").include("scenicSpotProductTransaction");
        List<ScenicSpotProductMPO> scenicSpotProductMPOS = mongoTemplate.find(query, ScenicSpotProductMPO.class);
        return scenicSpotProductMPOS;
    }

    @Override
    public List<ScenicSpotProductPriceMPO> queryProductPrice(String scenicSpotProductId, String startDate, String endDate) {
        return null;
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
    public ScenicSpotRuleMPO queryRuleById(String ruleId) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.and("_id").is(ruleId);
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, ScenicSpotRuleMPO.class);
    }

    @Override
    public ScenicSpotProductMPO querySpotProductById(String productId) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.and("_id").is(productId);
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
        criteria.and("scenicSpotProduct._id").is(productId);
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, ScenicSpotProductBackupMPO.class);
    }


}
