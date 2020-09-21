package com.huoli.trip.central.web.service.impl;

import com.huoli.trip.common.vo.request.BookCheckReq;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.order.CenterBookCheck;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2020/9/21<br>
 */
public class HltripOrderManager extends OrderManager{

    @Override
    public BaseResponse<CenterBookCheck> getCenterCheckInfos(BookCheckReq req) {

        return super.getCenterCheckInfos(req);
    }
}
