package com.huoli.trip.central.web.dao;

import com.huoli.trip.common.entity.Channel;

import java.util.List;

/**
 * 描述：<br/> 渠道
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：王德铭<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/30<br>
 */
public interface ChannelDao {
	List<Channel> queryChannelList();
}
