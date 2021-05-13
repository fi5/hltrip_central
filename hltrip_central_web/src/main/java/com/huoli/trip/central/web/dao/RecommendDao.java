package com.huoli.trip.central.web.dao;


import com.huoli.trip.common.entity.mpo.recommend.RecommendMPO;
import com.huoli.trip.common.vo.request.central.RecommendRequestV2;

import java.util.List;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2021/4/30<br>
 */
public interface RecommendDao {

    /**
     * 推荐列表
     * @param request
     * @return
     */
    RecommendMPO getList(RecommendRequestV2 request);

    /**
     * 获取城市
     * @param request
     * @return
     */
    List<RecommendMPO> getCites(RecommendRequestV2 request);
}
