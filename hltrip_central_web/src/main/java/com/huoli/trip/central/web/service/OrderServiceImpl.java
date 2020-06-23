package com.huoli.trip.central.web.service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.huoli.trip.central.api.OrderService;
import com.huoli.trip.common.vo.request.OrderStatusRequest;
import com.huoli.trip.supplier.api.YcfOrderStatusService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 中台订单服务
 * 提供统一的dubbo服务给客户端服务使用
 */
@Service(timeout = 10000,group = "hllx")
public class OrderServiceImpl implements OrderService {
    @Autowired
    OrderConsumerService orderConsumerService;

    @Override
     public Object getOrderStatus(OrderStatusRequest request){
        //渠道信息不可为空 返回相关提示
        String channelCode = request.getChannelCode();
        if(StringUtils.isEmpty(channelCode)){
            return null;
        }
        String orderId = request.getOrderId();
        if(StringUtils.isEmpty(channelCode)){
            return "请求参数订单号为空";
        }
        switch (channelCode){
            case "yaochufa":
                orderConsumerService.getYaochufaOrderStatus(orderId);
                break;
            case "difengyun":
                orderConsumerService.getDiFengYunOrderStatus(orderId);
                break;
            default:
                return "渠道信息不存在,请检查相关配置";
        }

        return null;
    }



}
