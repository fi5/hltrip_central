package com.huoli.trip.central.web.dao.impl;

import com.huoli.trip.central.web.dao.CityDao;
import com.huoli.trip.common.entity.CityPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
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
	public List<CityPO> queryCitys() {
		Query query = new Query();
		List<CityPO> citys = mongoTemplate.find(query, CityPO.class);
		return  citys;
	}
}
