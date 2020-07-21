package com.huoli.trip.central.web.service.impl;

import com.huoli.trip.central.api.BaseDataService;
import com.huoli.trip.central.web.dao.CityDao;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.entity.CityPO;
import com.huoli.trip.common.exception.HlCentralException;
import com.huoli.trip.common.vo.request.central.CityReq;
import com.huoli.trip.common.vo.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author :zhouwenbin
 * @time   :2020/7/2
 * @comment:
 **/
@Slf4j
@Service(timeout = 10000,group = "hltrip")
public class BaseDataServiceImpl implements BaseDataService {

	@Autowired
	CityDao cityDao;

	@Override
	public BaseResponse<List<CityPO>> queryCitys(CityReq req) {
		try {
			List<CityPO> citys = cityDao.queryCitys(req.getKeyword());
			//目前只有code与城市名
			return BaseResponse.success(citys);
		} catch (Exception e) {
			log.info("",e);
		}
		return BaseResponse.fail(CentralError.NO_RESULT_ERROR);

	}

	@Override
	public BaseResponse test() {
		throw  new HlCentralException(CentralError.ERROR_SERVER_ERROR);
//		return null;
	}
}
