package com.huoli.trip.central.web.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSONObject;
import com.huoli.trip.central.web.converter.CancelOrderConverter;
import com.huoli.trip.central.web.converter.CreateOrderConverter;
import com.huoli.trip.central.web.converter.OrderInfoTranser;
import com.huoli.trip.central.web.converter.PayOrderConverter;
import com.huoli.trip.central.web.dao.ProductDao;
import com.huoli.trip.central.web.util.CentralUtils;
import com.huoli.trip.central.web.util.DateUtils;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.constant.ChannelConstant;
import com.huoli.trip.common.vo.request.*;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.order.*;
import com.huoli.trip.supplier.api.YcfOrderService;
import com.huoli.trip.supplier.self.yaochufa.vo.*;
import com.huoli.trip.supplier.self.yaochufa.vo.basevo.YcfBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    @Autowired
    private ProductDao productDao;
    @Autowired
    private CreateOrderConverter createOrderConverter;
    @Autowired
    private PayOrderConverter payOrderConverter;
    @Autowired
    private CancelOrderConverter cancelOrderConverter;
    public final static String CHANNEL= ChannelConstant.SUPPLIER_TYPE_YCF;
    public String getChannel(){
        return CHANNEL;
    }
    public String test() {
        System.out.println("ycf");
        return "ycf";
    }
    public CenterBookCheck getNBCheckInfos(BookCheckReq req) throws RuntimeException{
        //中台输出
        CenterBookCheck centerBookCheck = new CenterBookCheck();
        //封装中台库存量
        List<Integer> stockList = new ArrayList<>();
        //销售价就是客户端传的
        centerBookCheck.setSalePrice(req.getSalePrice());
        String begin = req.getBeginDate();
        String end = req.getEndDate();
        //没传结束时间这样处理
        if(StringUtils.isBlank(req.getEndDate())){
            end = begin;
        }
        //供应商输出
        YcfBaseResult<YcfBookCheckRes> checkInfos = new YcfBaseResult();
        YcfBookCheckRes ycfBookCheckRes = null;
        //开始组装供应商请求参数
        YcfBookCheckReq ycfBookCheckReq = new YcfBookCheckReq();
        //转供应商productId
        ycfBookCheckReq.setProductId(CentralUtils.getSupplierId(req.getProductId()));
        try {
            ycfBookCheckReq.setBeginDate(DateUtils.parseTimeStringToDate(begin));
            ycfBookCheckReq.setEndDate(DateUtils.parseTimeStringToDate(end));
        } catch (ParseException e) {
            log.error("时间转换异常 ：{}",e);
        }
        try {
            checkInfos = ycfOrderService.getCheckInfos(ycfBookCheckReq);
            ycfBookCheckRes = checkInfos.getData();
//            //测试数据 start
//            String jsonString = "{\"data\":{\"productId\":\"16\",\"saleInfos\":[{\"date\":\"2016-06-14\",\"price\":99,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":2},{\"itemId\":\"321\",\"stock\":99}]},{\"date\":\"2016-06-15\",\"price\":98,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":2},{\"itemId\":\"321\",\"stock\":99}]},{\"date\":\"2016-06-16\",\"price\":97,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":10},{\"itemId\":\"321\",\"stock\":99}]},{\"date\":\"2016-06-17\",\"price\":96,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":0},{\"itemId\":\"321\",\"stock\":99}]},{\"date\":\"2016-06-18\",\"price\":95,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":2},{\"itemId\":\"321\",\"stock\":99}]}]},\"partnerId\":\"zx1000020160229\",\"success\":true,\"message\":null,\"statusCode\":200}";
//            YcfBaseResult ycfBaseResult = JSONObject.parseObject(jsonString,YcfBaseResult.class);
//            ycfBookCheckRes = JSONObject.parseObject(JSONObject.toJSONString(ycfBaseResult.getData()), YcfBookCheckRes.class);
//            //测试数据  end
            //供应商返回输入中台
            if(ycfBookCheckRes!=null){
                List<YcfBookSaleInfo> saleInfos = ycfBookCheckRes.getSaleInfos();
                //没有库存
                if(CollectionUtils.isEmpty(saleInfos)) {
                    centerBookCheck.setMessage(CentralError.NO_STOCK_ERROR.getError());
                    return centerBookCheck;
                }
                saleInfos.forEach(ycfBookSaleInfo -> {
                    stockList.add(ycfBookSaleInfo.getTotalStock());
                });
                //库存数排序从小到大，如果多个库存量就取最小库存数返回去就得了呗
                Collections.sort(stockList);
//                if(!CollectionUtils.isEmpty(stockList)&&stockList.size()>1){
//                    int min = stockList.stream().filter(stock -> stock>0).min(Comparator.naturalOrder()).orElse(null);
//                }
                //证明传的产品份数大于供应商库存最小剩余
                if(req.getCount()>stockList.get(0)){
                    centerBookCheck.setMessage(CentralError.NOTENOUGH_STOCK_ERROR.getError());
                    centerBookCheck.setStock(stockList.get(0));
                    log.info("传的产品份数大于库存剩余 产品编号：{}",req.getProductId());
                    return centerBookCheck;
                }
            }
        }catch (Exception e){
            log.error("ycfOrderService --> getNBCheckInfos rpc服务异常 :{}",e);
        }
//        //todo  销售价返回(按照份数来计算的  套餐里的天数是固定的)
//        centerBookCheck.setSettlePrice();
        centerBookCheck.setStock(stockList.get(0));
        centerBookCheck.setMessage(checkInfos.getMessage());
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
        //先校验是否可以预定
        BookCheckReq bookCheckReq = new BookCheckReq();
        bookCheckReq.setProductId(req.getProductId());
        bookCheckReq.setBeginDate(req.getBeginDate());
        bookCheckReq.setEndDate(req.getEndDate());
        bookCheckReq.setCount(req.getQunatity());
        //校验可查询预订
        if(this.getNBCheckInfos(bookCheckReq).getMessage() !=null){
            log.error("创建订单失败，预订前校验失败！产品编号：{}，不能创建订单",req.getProductId());
            return null;
        }
        //转换客户端传来的参数
        YcfCreateOrderReq ycfCreateOrderReq = createOrderConverter.convertRequestToSupplierRequest(req);
        //todo 查数据库组装供应商请求
//        ycfCreateOrderReq.setAmount();
//        YcfBookFood ycfBookFood = new YcfBookFood();
//        ycfBookFood.setFoodId();
//        ycfCreateOrderReq.setFoodDetail();
//        ycfCreateOrderReq.setPriceDetail();
//        ycfCreateOrderReq.setRoomDetail();
//        ycfCreateOrderReq.setSellAmount();
//        ycfCreateOrderReq.setTicketDetail();
        //供应商对象包装业务实体类
        CenterCreateOrderRes centerCreateOrderRes = new CenterCreateOrderRes();
        YcfCreateOrderRes ycfCreateOrderRes = new YcfCreateOrderRes();
//        try {
//            YcfBaseResult<YcfCreateOrderRes> ycfOrder = ycfOrderService.createOrder(ycfCreateOrderReq);
//            ycfCreateOrderRes = ycfOrder.getData();
//        }catch (Exception e){
//            log.error("ycfOrderService --> getNBCreateOrder rpc服务异常。。",e);
//            throw new RuntimeException("ycfOrderService --> rpc服务异常");
//        }
        //测试数据 start
        String jsonString = "{\"data\":{\"orderStatus\":0,\"orderId\":\"1234567890\"},\"success\":true,\"message\":null,\"partnerId\":\"zx1000020160229\",\"statusCode\":200}";
        YcfBaseResult ycfBaseResult = JSONObject.parseObject(jsonString,YcfBaseResult.class);
        ycfCreateOrderRes = JSONObject.parseObject(JSONObject.toJSONString(ycfBaseResult.getData()), YcfCreateOrderRes.class);
        //测试数据  end
        centerCreateOrderRes = createOrderConverter.convertSupplierResponseToResponse(ycfCreateOrderRes);
        //todo 通过查数据库封装中台结果集
        return centerCreateOrderRes;
    }

    public CenterPayOrderRes getCenterPayOrder(PayOrderReq req) throws RuntimeException{

        //转换前端传参
        YcfPayOrderReq ycfPayOrderReq = payOrderConverter.convertRequestToSupplierRequest(req);
        //封装中台创建订单返回结果
        CenterPayOrderRes centerPayOrderrRes = new CenterPayOrderRes();
        YcfPayOrderRes ycfPayOrderRes = new YcfPayOrderRes();
        try {
//            YcfBaseResult<YcfPayOrderRes> ycfPayOrder = ycfOrderService.payOrder(ycfPayOrderReq);
//            YcfPayOrderRes data = ycfPayOrder.getData();
//            ycfPayOrderRes.setOrderId(data.getOrderId());
//            ycfPayOrderRes.setOrderStatus(data.getOrderStatus());
        }catch (RuntimeException e){
            log.error("ycfOrderService --> getCenterPayOrder rpc服务异常。。",e);
            throw new RuntimeException("ycfOrderService --> rpc服务异常");
        }
        //测试数据 start
        String jsonString = "{\"data\":{\"orderStatus\":10,\"orderId\":\"1234567890\"},\"success\":true,\"message\":null,\"partnerId\":\"zx1000020160229\",\"statusCode\":200}";
        YcfBaseResult ycfBaseResult = JSONObject.parseObject(jsonString,YcfBaseResult.class);
        ycfPayOrderRes = JSONObject.parseObject(JSONObject.toJSONString(ycfBaseResult.getData()), YcfPayOrderRes.class);
        //测试数据  end
        //封装中台返回结果
        CenterPayOrderRes payOrderRes = payOrderConverter.convertSupplierResponseToResponse(ycfPayOrderRes);
        //组装本地订单号参数
        payOrderRes.setLocalOrderId(req.getPartnerOrderId());
        return payOrderRes;
    }

    public CenterCancelOrderRes getCenterCancelOrder(CancelOrderReq req) throws RuntimeException{
        //转换前端传参
        YcfCancelOrderReq ycfCancelOrderReq = cancelOrderConverter.convertRequestToSupplierRequest(req);
        //封装中台返回结果
        CenterCancelOrderRes centerCancelOrderRes = new CenterCancelOrderRes();
        YcfCancelOrderRes ycfCancelOrderRes = new YcfCancelOrderRes();
//        try {
//            YcfBaseResult<YcfCancelOrderRes> ycfBaseResult = ycfOrderService.cancelOrder(ycfCancelOrderReq);
//            ycfCancelOrderRes = ycfBaseResult.getData();
//        }catch (RuntimeException e){
//            log.error("ycfOrderService --> getCenterPayOrder rpc服务异常。。",e);
//            throw new RuntimeException("ycfOrderService --> rpc服务异常");
//        }
        //测试数据 start
        String jsonString = "{\"data\":{\"orderStatus\":null,\"orderId\":\"45775553335\",\"async\":1},\"success\":true,\"message\":null,\"partnerId\":\"zx1000020160229\",\"statusCode\":200}";
        YcfBaseResult ycfBaseResult = JSONObject.parseObject(jsonString,YcfBaseResult.class);
        ycfCancelOrderRes = JSONObject.parseObject(JSONObject.toJSONString(ycfBaseResult.getData()), YcfCancelOrderRes.class);
        //测试数据  end
        //组装中台返回结果
        CenterCancelOrderRes cancelOrderRes = cancelOrderConverter.convertSupplierResponseToResponse(ycfCancelOrderRes);
        return cancelOrderRes;
    }
}
