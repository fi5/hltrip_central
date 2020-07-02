package com.huoli.trip.central.web.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.huoli.trip.central.api.IBaseDataService;
import com.huoli.trip.central.web.dao.CityDao;
import com.huoli.trip.common.entity.CityPO;
import com.huoli.trip.common.vo.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author :zhouwenbin
 * @time   :2020/7/2
 * @comment:
 **/
@Slf4j
@Service(timeout = 10000,group = "hltrip")
public class BaseDataServiceImpl implements IBaseDataService{

	@Autowired
	CityDao cityDao;

	@Override
	public BaseResponse<List<CityPO>> queryCitys() {
		List<CityPO> citys = cityDao.queryCitys();
		return BaseResponse.success(citys);
	}
}
