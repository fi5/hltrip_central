package com.huoli.trip.central.web.dao.impl;

import com.huoli.trip.central.web.dao.HotelScenicSpotProductDao;
import com.huoli.trip.common.entity.mpo.hotelScenicSpot.HotelScenicSpotProductMPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2021/6/1<br>
 */
@Repository
public class HotelScenicSpotProductDaoImpl implements HotelScenicSpotProductDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public HotelScenicSpotProductMPO getProductById(String productId){
        return mongoTemplate.findOne(new Query(Criteria.where("_id").is(productId)), HotelScenicSpotProductMPO.class);
    }
}
