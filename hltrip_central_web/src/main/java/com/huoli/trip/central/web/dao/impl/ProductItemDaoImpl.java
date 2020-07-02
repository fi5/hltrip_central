package com.huoli.trip.central.web.dao.impl;

import com.huoli.trip.central.web.dao.ProductItemDao;
import com.huoli.trip.common.entity.ProductItemPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
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
public class ProductItemDaoImpl implements ProductItemDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<ProductItemPO> selectByCityAndType(String city, Integer type, int pageSize){
        Query query = new Query(Criteria.where("city").is(city).and("itemType").is(type)).limit(pageSize);
        List<ProductItemPO> productItems = mongoTemplate.find(query, ProductItemPO.class);
        return productItems;
    }

    @Override
    public ProductItemPO selectByCode(String code){
        Query query = new Query(Criteria.where("code").is(code));
        ProductItemPO productItem = mongoTemplate.findOne(query, ProductItemPO.class);
        return productItem;
    }

}
