package com.huoli.trip.central.web.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSONObject;
import com.huoli.trip.common.vo.request.BookCheckReq;
import com.huoli.trip.common.vo.request.OrderOperReq;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.order.OrderDetailRep;
import com.huoli.trip.supplier.api.YcfOrderService;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfBookCheckReq;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfBookCheckRes;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfOrderStatusResult;
import com.huoli.trip.supplier.self.yaochufa.vo.YcfVouchersResult;
import com.huoli.trip.supplier.self.yaochufa.vo.basevo.YcfBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 描述：desc<br>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：顾刘川<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/1<br>
 */
@Component
@Slf4j
public class YcfOrderManger extends OrderManager {
    @Reference(group = "hllx")
    private YcfOrderService ycfOrderService;
    public final static String CHANNEL="ycf";
    public String getChannel(){
        return CHANNEL;
    }
    public String test() {
        System.out.println("ycf");
        return "ycf";
    }
    public Object getNBCheckInfos(BookCheckReq req) throws Exception {
        YcfBaseResult<YcfBookCheckRes> checkInfos = new YcfBaseResult<>();
        YcfBookCheckReq reqest = new YcfBookCheckReq();
        BeanUtils.copyProperties(req,reqest);
        try {
            checkInfos = ycfOrderService.getCheckInfos(reqest);
        }catch (Exception e){
            throw new RuntimeException("ycfOrderService --> rpc服务异常。。",e);
        }
        return checkInfos.getData();
    }

   public BaseResponse<OrderDetailRep> getOrderDetail(OrderOperReq req){

        final YcfBaseResult<YcfOrderStatusResult> order = ycfOrderService.getOrder(req.getOrderId());
        try {
            final YcfOrderStatusResult data = order.getData();
            //如果数据为空,直接返回错
            if(data==null || !order.getSuccess())
                return BaseResponse.fail(-100,order.getMessage(),null);
            OrderDetailRep rep=new OrderDetailRep();
            rep.setOrderId(data.getOrderId());
            rep.setOrderStatus(data.getOrderStatus());
            rep.setVochers(JSONObject.parseArray(JSONObject.toJSONString(data.getVochers()), OrderDetailRep.Voucher.class));
            return BaseResponse.success(rep);
        } catch (Exception e) {
            log.info("",e);
            return BaseResponse.fail(-100,order.getMessage(),null);
        }

    }

    public BaseResponse<OrderDetailRep> getVochers(OrderOperReq req){

        final YcfBaseResult<YcfVouchersResult> vochers = ycfOrderService.getVochers(req.getOrderId());
        try {
            final YcfVouchersResult data = vochers.getData();
            OrderDetailRep rep=new OrderDetailRep();
            rep.setOrderId(req.getOrderId());
            rep.setVochers(JSONObject.parseArray(JSONObject.toJSONString(data.getVochers()), OrderDetailRep.Voucher.class));
            return BaseResponse.success(rep);
        } catch (Exception e) {
            log.info("",e);
            return BaseResponse.fail(-101,vochers.getMessage(),null);
        }

    }
}
