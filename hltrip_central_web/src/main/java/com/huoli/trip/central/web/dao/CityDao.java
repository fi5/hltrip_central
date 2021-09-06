package com.huoli.trip.central.web.dao;

import com.huoli.trip.common.entity.CityPO;

import java.util.List;

/**
 * @author :zhouwenbin
 * @time   :2020/7/2
 * @comment:
 **/
public interface CityDao {
	List<CityPO> queryCitys(String keyWord);

	List<CityPO> queryCitys(String keyWord, int limit);
}
