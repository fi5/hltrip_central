package com.huoli.trip.central.web.dao.impl;

import com.huoli.trip.central.web.dao.ChannelDao;
import com.huoli.trip.common.entity.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 描述: <br> 渠道列表
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：王德铭<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/30<br>
 */
@Repository
public class ChannelDaoImpl implements ChannelDao {

    @Autowired
    private MongoTemplate mongoTemplate;
    @Override
    public List<Channel> queryChannelList() {
        return mongoTemplate.findAll(Channel.class);
    }
}
