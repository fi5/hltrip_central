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
        if(StringUtils.isNotBlank(increasePrice.getChannelCode())){
            supplierId.orOperator(Criteria.where("supplierId").is(increasePrice), Criteria.where("supplierId").is(null));
        } else {
            supplierId.andOperator(Criteria.where("supplierId").is(null));
        }
        Criteria appSource = new Criteria();
        if(StringUtils.isNotBlank(increasePrice.getAppSource())){
            appSource.orOperator(Criteria.where("appSource").in(increasePrice.getAppSource()), Criteria.where("appSource").is(null));
        } else {
            appSource.andOperator(Criteria.where("appSource").is(null));
        }
        Criteria category = new Criteria();
        if(StringUtils.isNotBlank(increasePrice.getProductCategory())){
            category.orOperator(Criteria.where("productType").in(increasePrice.getProductCategory()), Criteria.where("productType").is(null));
        } else {
            category.andOperator(Criteria.where("productType").is(null));
        }
        Criteria criteria = new Criteria();
        criteria.andOperator(supplierId, appSource, category);
        Query query = new Query();
        return mongoTemplate.find(Query.query(criteria), SupplierPolicyPO.class);
    }

}
