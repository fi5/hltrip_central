package com.huoli.trip.central.web.dao.impl;

import com.huoli.trip.central.web.dao.RecommendDao;
import com.huoli.trip.common.constant.MongoConst;
import com.huoli.trip.common.entity.mpo.recommend.RecommendMPO;
import com.huoli.trip.common.vo.request.central.RecommendRequestV2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
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
 * 创建日期：2021/4/30<br>
 */
@Repository
public class RecommendDaoImpl implements RecommendDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public RecommendMPO getList(RecommendRequestV2 request){
        Criteria criteria = Criteria.where("position").is(request.getPosition().toString())
                .and("city").is(StringUtils.isBlank(request.getCity()) ? "0" : request.getCity());
        return mongoTemplate.findOne(new Query(criteria), RecommendMPO.class);
    }

    @Override
    public RecommendMPO getListByPosition(RecommendRequestV2 request){
        Criteria criteria = Criteria.where("position").is(request.getPosition().toString());
        return mongoTemplate.findOne(new Query(criteria), RecommendMPO.class);
    }

    @Override
    public List<RecommendMPO> getListByTag(String position, List<String> tags){
        Criteria criteria = Criteria.where("position").is(position).and("recommendBaseInfos.title").in(tags);
        return mongoTemplate.find(new Query(criteria), RecommendMPO.class);
    }

    @Override
    public List<RecommendMPO> getCites(RecommendRequestV2 request){
        Criteria criteria = Criteria.where("position").is(request.getPosition().toString());
        MatchOperation matchOperation = Aggregation.match(criteria);
        GroupOperation groupOperation = Aggregation.group("city")
                .first("city").as("city")
                .first("cityName").as("cityName");
        ProjectionOperation projectionOperation = Aggregation.project("city", "cityName");
        Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, projectionOperation);
        AggregationResults<RecommendMPO> aggregationResults = mongoTemplate.aggregate(aggregation, MongoConst.COLLECTION_NAME_RECOMMEND_HUOLI, RecommendMPO.class);
        return aggregationResults.getMappedResults();
    }
}
