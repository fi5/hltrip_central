package com.huoli.trip.central.web.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSONObject;
import com.huoli.trip.central.web.converter.OrderInfoTranser;
import com.huoli.trip.common.constant.ChannelConstant;
import com.huoli.trip.common.vo.request.BookCheckReq;
import com.huoli.trip.common.vo.request.CreateOrderReq;
import com.huoli.trip.common.vo.request.OrderOperReq;
import com.huoli.trip.common.vo.request.PayOrderReq;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.order.CenterBookCheckRes;
import com.huoli.trip.common.vo.response.order.CenterCreateOrderRes;
import com.huoli.trip.common.vo.response.order.CenterPayOrderRes;
import com.huoli.trip.common.vo.response.order.OrderDetailRep;
import com.huoli.trip.supplier.api.YcfOrderService;
import com.huoli.trip.supplier.self.yaochufa.vo.*;
import com.huoli.trip.supplier.self.yaochufa.vo.basevo.YcfBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.Date;

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
    public final static String CHANNEL= ChannelConstant.SUPPLIER_TYPE_YCF;
    public String getChannel(){
        return CHANNEL;
    }
    public String test() {
        System.out.println("ycf");
        return "ycf";
    }
    public CenterBookCheckRes.CenterBookCheck getNBCheckInfos(BookCheckReq req) throws RuntimeException{
        YcfBookCheckReq reqest = new YcfBookCheckReq();
        BeanUtils.copyProperties(req,reqest);
        CenterBookCheckRes.CenterBookCheck centerBookCheck = new CenterBookCheckRes.CenterBookCheck();
        try {
//            CenterBookCheckRes.CenterBookCheck centerBookCheck = new CenterBookCheckRes.CenterBookCheck();
//            YcfBaseResult<YcfBookCheckRes> checkInfos = ycfOrderService.getCheckInfos(reqest);
//            YcfBookCheckRes data = checkInfos.getData();
//            centerBookCheck.setProductId(data.getProductId());
            //todo 校验产品份数查询mongon库存量
            if(req.getCount()>10){
                centerBookCheck.setProductId(req.getProductId());
                centerBookCheck.setMessage("超过该产品库存量，不能预订");
                centerBookCheck.setErrorCode("001");
                centerBookCheck.setProductCount(10);
            }
            //测试数据 start
            String jsonString = "{\"data\":{\"productId\":\"16\",\"saleInfos\":[{\"date\":\"2016-06-14\",\"price\":99,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":2},{\"itemId\":\"321\",\"stock\":99}]},{\"date\":\"2016-06-15\",\"price\":98,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":2},{\"itemId\":\"321\",\"stock\":99}]},{\"date\":\"2016-06-16\",\"price\":97,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":10},{\"itemId\":\"321\",\"stock\":99}]},{\"date\":\"2016-06-17\",\"price\":96,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":0},{\"itemId\":\"321\",\"stock\":99}]},{\"date\":\"2016-06-18\",\"price\":95,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":2},{\"itemId\":\"321\",\"stock\":99}]}]},\"partnerId\":\"zx1000020160229\",\"success\":true,\"message\":null,\"statusCode\":200}";
            YcfBaseResult ycfBaseResult = JSONObject.parseObject(jsonString,YcfBaseResult.class);
            if(ycfBaseResult!=null){
                centerBookCheck.setProductId(req.getProductId());
                //todo mongo里查价格日历库存量
                centerBookCheck.setProductCount(10);
//            centerBookCheck = JSONObject.parseObject(JSONObject.toJSONString(ycfBaseResult.getData()), CenterBookCheckRes.CenterBookCheck.class);
            }
            System.out.println(centerBookCheck);
            //测试数据  end
            //todo 封装供应商返回



        }catch (Exception e){
            log.error("ycfOrderService --> getNBCheckInfos rpc服务异常。。",e);
            throw new RuntimeException("ycfOrderService --> rpc服务异常");
        }
        return centerBookCheck;
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
            rep.setOrderStatus(OrderInfoTranser.genCommonOrderStatus(data.getOrderStatus(),1));
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

    public CenterCreateOrderRes getNBCreateOrder(CreateOrderReq req) throws RuntimeException{
        YcfCreateOrderReq reqest = new YcfCreateOrderReq();
        BeanUtils.copyProperties(req,reqest);
        //todo 封装客户端传来的参数
        //先校验是否可以预定
        BookCheckReq bookCheckReq = new BookCheckReq();
        bookCheckReq.setChannelCode(req.getChannelCode());
        bookCheckReq.setProductId(req.getProductId());
        //校验可查询预订
        if(this.getNBCheckInfos(bookCheckReq).getErrorCode() !=null){
            log.error("该产品编号没有订单可以预定 产品编号 ：{}",req.getProductId());
            return null;
        }
        //封装中台创建订单返回结果
        CenterCreateOrderRes centerCreateOrderRes = new CenterCreateOrderRes();
        CenterCreateOrderRes.Supplier supplier = new CenterCreateOrderRes.Supplier();
        CenterCreateOrderRes.CreateOrderRes ycfCreateOrderRes = new CenterCreateOrderRes.CreateOrderRes();
        try {
//            YcfBaseResult<YcfCreateOrderRes> ycfOrder = ycfOrderService.createOrder(reqest);
//            YcfCreateOrderRes data = ycfOrder.getData();
//            createOrderRes.setOrderId(data.getOrderId());
//            createOrderRes.setOrderStatus(data.getOrderStatus());
            //测试数据 start
            String jsonString = "{\"data\":{\"orderStatus\":0,\"orderId\":\"1234567890\"},\"success\":true,\"message\":null,\"partnerId\":\"zx1000020160229\",\"statusCode\":200}";
            YcfBaseResult ycfBaseResult = JSONObject.parseObject(jsonString,YcfBaseResult.class);
            ycfCreateOrderRes = JSONObject.parseObject(JSONObject.toJSONString(ycfBaseResult.getData()), CenterCreateOrderRes.CreateOrderRes.class);
            //测试数据  end
            supplier.setCreateOrderObj(ycfCreateOrderRes);
            supplier.setType(req.getChannelCode());
            centerCreateOrderRes.setSupplier(supplier);
        }catch (Exception e){
            log.error("ycfOrderService --> getNBCreateOrder rpc服务异常。。",e);
            throw new RuntimeException("ycfOrderService --> rpc服务异常");
        }
        return centerCreateOrderRes;
    }

    public CenterPayOrderRes getCenterPayOrder(PayOrderReq req) throws RuntimeException{
        YcfPayOrderReq reqest = new YcfPayOrderReq();
        //组装支付流水参数
        req.setPaySerialNumber(serialNumber(req.getPartnerOrderId()));
        BeanUtils.copyProperties(req,reqest);
        //封装前端传参

        //封装中台创建订单返回结果
        CenterPayOrderRes centerPayOrderrRes = new CenterPayOrderRes();
        CenterPayOrderRes.Supplier supplier = new CenterPayOrderRes.Supplier();
        CenterPayOrderRes.PayOrderRes ycfPayOrderRes = new CenterPayOrderRes.PayOrderRes();
        try {
//            YcfBaseResult<YcfPayOrderRes> ycfPayOrder = ycfOrderService.payOrder(reqest);
//            YcfPayOrderRes data = ycfPayOrder.getData();
//            ycfPayOrderRes.setOrderId(data.getOrderId());
//            ycfPayOrderRes.setOrderStatus(data.getOrderStatus());
            //测试数据 start
            String jsonString = "{\"data\":{\"orderStatus\":10,\"orderId\":\"1234567890\"},\"success\":true,\"message\":null,\"partnerId\":\"zx1000020160229\",\"statusCode\":200}";
            YcfBaseResult ycfBaseResult = JSONObject.parseObject(jsonString,YcfBaseResult.class);
            ycfPayOrderRes = JSONObject.parseObject(JSONObject.toJSONString(ycfBaseResult.getData()), CenterPayOrderRes.PayOrderRes.class);
            //测试数据  end
            supplier.setPayOrderObj(ycfPayOrderRes);
            supplier.setType(req.getChannelCode());
            centerPayOrderrRes.setSupplier(supplier);
        }catch (Exception e){
            log.error("ycfOrderService --> getCenterPayOrder rpc服务异常。。",e);
            throw new RuntimeException("ycfOrderService --> rpc服务异常");
        }
        return centerPayOrderrRes;
    }

    //生成支付流水号
    private static String serialNumber(String orderNo) {
        return String.valueOf(new Date().getTime())+Math.abs(orderNo.hashCode());
    }
}
