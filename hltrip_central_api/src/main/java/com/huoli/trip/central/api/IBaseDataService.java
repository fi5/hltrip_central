package com.huoli.trip.central.api;

import com.huoli.trip.common.entity.CityPO;

import java.util.List;

/**
 * @author :zhouwenbin
 * @time   :2020/7/2
 * @comment:如城市选择
 **/
public interface IBaseDataService {

	 List<CityPO> queryCitys();

}
