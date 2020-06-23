package com.huoli.trip.central.service;

import com.huoli.trip.common.vo.request.OrderStatusRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 中台订单服务
 */
@Service
public class OrderService {

    private Object getOrderStatus(OrderStatusRequest request){
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
                getYaochufaOrderStatus(orderId);
                break;
            case "difengyun":
                getDiFengYunOrderStatus(orderId);
                break;
            default:
                return "渠道信息不存在,请检查相关配置";
        }

        return null;
    }

    private Object getYaochufaOrderStatus(String orderId){
        return null;

    }

    private void getDiFengYunOrderStatus(String orderId){
        return;
    }

}
