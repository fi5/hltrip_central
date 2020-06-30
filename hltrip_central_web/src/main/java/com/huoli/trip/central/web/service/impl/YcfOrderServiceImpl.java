package com.huoli.trip.central.web.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.huoli.trip.central.api.OrderService;
import com.huoli.trip.central.web.service.OrderConsumerService;
import com.huoli.trip.common.constant.ChannelConstant;
import com.huoli.trip.common.vo.request.BookCheckReq;
import com.huoli.trip.common.vo.request.OrderStatusRequest;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfBookCheckReq;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 中台订单服务
 * 提供统一的dubbo服务给客户端服务使用
 */
@Service(timeout = 10000,group = "hllx")
public class YcfOrderServiceImpl implements OrderService {
    @Autowired
    OrderConsumerService orderConsumerService;

    @Override
     public Object getOrderStatus(OrderStatusRequest request){
        //渠道信息不可为空 返回相关提示
        String channelCode = request.getChannelCode();
        if(StringUtils.isEmpty(channelCode)){
            return "渠道信息不可为空 返回相关提示";
        }
        String orderId = request.getOrderId();
        if(StringUtils.isEmpty(orderId)){
            return "请求参数订单号为空";
        }
        switch (request.getChannelCode()){
            case ChannelConstant.SUPPLIER_TYPE_YCF:
                orderConsumerService.getYaochufaOrderStatus(orderId);
                break;
            case ChannelConstant.SUPPLIER_TYPE_DFY:
                orderConsumerService.getDiFengYunOrderStatus(orderId);
                break;
            default:
                return "渠道信息不存在,请检查相关配置";
        }

        return null;
    }

    @Override
    public Object getCheckInfos(BookCheckReq request) {
        YcfBookCheckReq req = new YcfBookCheckReq();
        BeanUtils.copyProperties(request,req);
        return orderConsumerService.getYcfCheckInfos(req);
    }

}
