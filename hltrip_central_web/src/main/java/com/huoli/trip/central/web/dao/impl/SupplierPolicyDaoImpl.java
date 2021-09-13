package com.huoli.trip.central.web.dao.impl;

import com.huoli.trip.central.web.dao.SupplierPolicyDao;
import com.huoli.trip.common.entity.SupplierPolicyPO;
import com.huoli.trip.common.vo.IncreasePrice;
import org.apache.commons.lang3.StringUtils;
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
 * 创建日期：2021/1/6<br>
 */
@Repository
public class SupplierPolicyDaoImpl implements SupplierPolicyDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public SupplierPolicyPO getSupplierPolicyBySupplierId(String supplierId){
        return mongoTemplate.findOne(Query.query(Criteria.where("supplierId").is(supplierId)), SupplierPolicyPO.class);
    }

    @Override
    public List<SupplierPolicyPO> getSupplierPolicy(IncreasePrice increasePrice){
        Criteria supplierId = new Criteria();
        supplierId.orOperator(Criteria.where("supplierId").is(increasePrice.getChannelCode()), Criteria.where("supplierId").is(null));
        Criteria appSource = new Criteria();
        appSource.orOperator(Criteria.where("appSource").in(increasePrice.getAppSource()), Criteria.where("appSource").is(null));
        Criteria category = new Criteria();
        category.orOperator(Criteria.where("productType").in(increasePrice.getProductCategory()), Criteria.where("productType").is(null));
        Criteria appSubSource = new Criteria();
        appSubSource.orOperator(Criteria.where("appSubSource").in(increasePrice.getAppSubSource()), Criteria.where("appSubSource").is(null));
        Criteria scenicSpotId = new Criteria();
        scenicSpotId.orOperator(Criteria.where("scenicSpotId").in(increasePrice.getAppSubSource()), Criteria.where("scenicSpotId").is(null));
        Criteria criteria = new Criteria();
        criteria.andOperator(supplierId, appSource, category, appSubSource);
        return mongoTemplate.find(Query.query(criteria), SupplierPolicyPO.class);
    }

}
