package com.huoli.trip.central.web.service.impl;

import com.huoli.trip.central.api.BaseDataService;
import com.huoli.trip.central.web.dao.ChannelDao;
import com.huoli.trip.central.web.dao.CityDao;
import com.huoli.trip.central.web.dao.ProductDao;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.entity.Channel;
import com.huoli.trip.common.entity.CityPO;
import com.huoli.trip.common.exception.HlCentralException;
import com.huoli.trip.common.vo.request.central.CityReq;
import com.huoli.trip.common.vo.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @author :zhouwenbin
 * @time   :2020/7/2
 * @comment:
 **/
@Slf4j
@Service(timeout = 10000, group = "hltrip")
public class BaseDataServiceImpl implements BaseDataService {

	@Autowired
	CityDao cityDao;
	@Autowired
	private ChannelDao channelDao;
	@Autowired
	ProductDao productDao;
	HashMap<String, String> validCitys = null;

	@Override
	public BaseResponse<List<CityPO>> queryCitys(CityReq req) {
		try {
			List<CityPO> citys = cityDao.queryCitys(req.getKeyword());

//			if (validCitys == null) {
				try {
					validCitys = productDao.queryValidCitys();
				} catch (Exception e) {
					log.error("信息{}", e);
				}
//			}
			final Iterator<CityPO> iterator = citys.iterator();
			while(iterator.hasNext()){
				CityPO entry = iterator.next();
				if(null!=validCitys&&validCitys.get(entry.getCityName())==null)
					iterator.remove();

				}
			//目前只有code与城市名
			return BaseResponse.success(citys);
		} catch (Exception e) {
			log.error("", e);
		}
		return BaseResponse.fail(CentralError.NO_RESULT_ERROR);

	}

	@Override
	public BaseResponse test() {
		throw new HlCentralException(CentralError.ERROR_SERVER_ERROR);
//		return null;
	}

	@Override
	public BaseResponse<List<Channel>> queryChannelList() {
		try {
			List<Channel> channels = channelDao.queryChannelList();
			return BaseResponse.success(channels);
		} catch (Exception e) {
			log.error("获取渠道列表失败 ：{}", e);
		}
		return BaseResponse.fail(CentralError.NO_RESULT_ERROR);
	}

	@Override
	public void reSetCity() {
		validCitys=null;
		try {
			validCitys = productDao.queryValidCitys();
		} catch (Exception e) {
			log.error("信息{}",e);
		}
	}
}
