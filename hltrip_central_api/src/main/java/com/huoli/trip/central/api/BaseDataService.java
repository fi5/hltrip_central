package com.huoli.trip.central.api;

import com.huoli.trip.common.entity.Channel;
import com.huoli.trip.common.entity.CityPO;
import com.huoli.trip.common.vo.request.central.CityReq;
import com.huoli.trip.common.vo.response.BaseResponse;

import java.util.List;

/**
 * @author :zhouwenbin
 * @time   :2020/7/2
 * @comment:如城市选择
 **/
public interface BaseDataService {

	/**
	 * 城市信息
	 * @return
	 */
	 BaseResponse<List<CityPO>> queryCitys(CityReq req);

	 BaseResponse test();

	/**
	 * 描述：<br/> 获取所有产品渠道
	 * 版权：Copyright (c) 2011-2020<br>
	 * 公司：活力天汇<br>
	 * 作者：王德铭<br>
	 * 版本：1.0<br>
	 * 创建日期：2020/7/30<br>
	 */
	BaseResponse<List<Channel>> queryChannelList();

}
