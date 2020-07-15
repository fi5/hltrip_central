package com.huoli.trip.central.web.dao.impl;

import com.huoli.trip.central.web.converter.ProductConverter;
import com.huoli.trip.central.web.dao.ProductItemDao;
import com.huoli.trip.common.entity.ProductItemPO;
import com.huoli.trip.common.vo.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.Sphere;
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
    public List<ProductItemPO> getByCityAndType(String city, Integer type, int pageSize){
        Query query = new Query(Criteria.where("city").is(city).and("itemType").is(type)).limit(pageSize);
        List<ProductItemPO> productItems = mongoTemplate.find(query, ProductItemPO.class);
        return productItems;
    }

    @Override
    public ProductItemPO getByCode(String code){
        Query query = new Query(Criteria.where("code").is(code));
        ProductItemPO productItem = mongoTemplate.findOne(query, ProductItemPO.class);
        return productItem;
    }

    @Override
    public ProductItemPO getImagesByCode(String code){
        Query query = new Query(Criteria.where("code").is(code));
        query.fields().include("images");
        return mongoTemplate.findOne(query, ProductItemPO.class);
    }

    @Override
    public List<ProductItemPO> getByCoordinate(int productType, Coordinate coordinate, double radius){
        int itemType = ProductConverter.getItemType(productType);
        Point point = new Point(coordinate.getLongitude(), coordinate.getLatitude());
        Sphere sphere = new Sphere(point, new Distance(radius, Metrics.KILOMETERS));
        Query query = new Query(Criteria.where("itemType").is(itemType).and("itemCoordinate").within(sphere));
        return mongoTemplate.find(query, ProductItemPO.class);
    }

    @Override
    public List<ProductItemPO> getByCity(int productType, String city){
        int itemType = ProductConverter.getItemType(productType);
        Query query = new Query(Criteria.where("itemType").is(itemType).and("city").is(city));
        return mongoTemplate.find(query, ProductItemPO.class);
    }
}
