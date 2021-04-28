package com.huoli.trip.central.web.dao.impl;


import com.huoli.trip.central.web.dao.ScenicSpotDao;
import com.huoli.trip.common.entity.mpo.ProductListMPO;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @time   :2021/3/19
 * @comment:
 **/
@Repository
public class ScenicSpotDaoImpl implements ScenicSpotDao {

    @Autowired
    private MongoTemplate mongoTemplate;


    @Override
    public ScenicSpotMPO qyerySpotById(String scenicSpotId) {
        Query query = new Query(Criteria.where("_id").is(scenicSpotId));
        query.fields().include("_id").include("name").include("address").include("level").include("operatingStatus").include("coordinate").include("scenicSpotOpenTimes").include("otherOpenTimeDesc").include("briefDesc");
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
        return null;
    }

    @Override
    public ScenicSpotPayServiceMPO querySpotPayItem(String spotPayItemId) {
        return null;
    }

    @Override
    public List<ScenicSpotProductMPO> querySpotProduct(String scenicSpotId, String date) {
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


}
