package com.huoli.trip.central.web.dao.impl;

import com.huoli.trip.central.web.dao.ScenicProductSortDao;
import com.huoli.trip.central.web.dao.ScenicSpotRuleDao;
import com.huoli.trip.common.entity.mpo.ScenicProductSortMPO;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.ScenicSpotProductMPO;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.ScenicSpotRuleMPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public class ScenicProductSortDaoImpl implements ScenicProductSortDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<ScenicProductSortMPO> queryScenicProductSortByScenicId(String scenicId){
        return mongoTemplate.find(new Query(Criteria.where("scenicSpotId").is(scenicId)).with(Sort.by("sort")), ScenicProductSortMPO.class);
    }
}
