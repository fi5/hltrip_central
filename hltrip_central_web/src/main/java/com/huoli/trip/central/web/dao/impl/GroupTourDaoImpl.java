package com.huoli.trip.central.web.dao.impl;

import com.huoli.trip.central.web.dao.GroupTourDao;
import com.huoli.trip.common.entity.mpo.groupTour.GroupTourProductMPO;
import com.huoli.trip.common.entity.mpo.groupTour.GroupTourProductSetMealMPO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author zhouwenbin
 * @version 1.0
 * @date 2021/5/9
 */
@Repository
@Slf4j
public class GroupTourDaoImpl implements GroupTourDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public GroupTourProductMPO queryTourProduct(String groupTourId) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.and("_id").is(groupTourId);
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, GroupTourProductMPO.class);
    }

    @Override
    public List<GroupTourProductSetMealMPO> queryProductSetMealByProductId(String productId, List<String> depCodes, String packageId) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.and("groupTourProductId").is(productId);
        if(!CollectionUtils.isEmpty(depCodes)){
            criteria.and("depCode").in(depCodes);
        }
        if(StringUtils.isNotBlank(packageId)){
            criteria.and("_id").is(packageId);
        }
        query.addCriteria(criteria);
        return mongoTemplate.find(query, GroupTourProductSetMealMPO.class);
    }

    @Override
    public GroupTourProductSetMealMPO queryGroupSetMealBySetId(String setMealId) {
        Query query = new Query();
        Criteria criteria = new Criteria();
        criteria.and("_id").is(setMealId);
        query.addCriteria(criteria);
        return mongoTemplate.findOne(query, GroupTourProductSetMealMPO.class);
    }
}
