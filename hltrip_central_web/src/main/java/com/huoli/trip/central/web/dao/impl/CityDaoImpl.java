package com.huoli.trip.central.web.dao.impl;

import com.huoli.trip.central.web.dao.CityDao;
import com.huoli.trip.central.web.util.CentralUtils;
import com.huoli.trip.common.entity.CityPO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author :zhouwenbin
 * @time   :2020/7/2
 * @comment:
 **/
@Repository
public class CityDaoImpl implements CityDao{

	@Autowired
	private MongoTemplate mongoTemplate;


	@Override
	public List<CityPO> queryCitys(String keyWord) {
		Query query = new Query();
		if(StringUtils.isNotBlank(keyWord)){
			if(CentralUtils.isChinese(keyWord.charAt(0))){
				query=new Query(Criteria.where("cityName").regex(keyWord));
			}else{
				Criteria criteria = new Criteria();
				criteria.orOperator(Criteria.where("pinyin").regex(keyWord),Criteria.where("jianpin").regex(keyWord),Criteria.where("code").regex(keyWord));
			    query.addCriteria(criteria);
			}
		}
		List<CityPO> citys = mongoTemplate.find(query, CityPO.class);
		return  citys;
	}
}
