package com.huoli.trip.central.web.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSONObject;
import com.huoli.trip.central.web.converter.CreateOrderConverter;
import com.huoli.trip.central.web.converter.OrderInfoTranser;
import com.huoli.trip.central.web.converter.PayOrderConverter;
import com.huoli.trip.central.web.dao.ProductDao;
import com.huoli.trip.central.web.util.CentralUtils;
import com.huoli.trip.central.web.util.DateUtils;
import com.huoli.trip.common.constant.ChannelConstant;
import com.huoli.trip.common.entity.PriceInfoPO;
import com.huoli.trip.common.entity.PricePO;
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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
    public final static String CHANNEL= ChannelConstant.SUPPLIER_TYPE_YCF;
    public String getChannel(){
        return CHANNEL;
    }
    public String test() {
        System.out.println("ycf");
        return "ycf";
    }
    public CenterBookCheckRes.CenterBookCheck getNBCheckInfos(BookCheckReq req) throws RuntimeException{
        //中台输出
        CenterBookCheckRes.CenterBookCheck centerBookCheck = new CenterBookCheckRes.CenterBookCheck();
        centerBookCheck.setProductId(req.getProductId());
        List<CenterBookCheckRes.ProductStock> productStockList = new ArrayList<>();
        //todo 校验产品份数查询mongo库存量
        if(req.getCount()>5){
            centerBookCheck.setMessage("超过该产品库存量，不能预订");
            centerBookCheck.setErrorCode("002");
            //todo 刷新库存逻辑

            return centerBookCheck;
        }
        String begin = req.getBeginDate();
        String end = req.getEndDate();
        if(StringUtils.isBlank(req.getEndDate())){
            end = begin;
        }
        Date beginDate = null;
        Date endDate = null;
        try {
            beginDate = DateUtils.parseTimeStringToDate2(begin);
            endDate = DateUtils.parseTimeStringToDate2(end);
        } catch (ParseException e) {
            log.error("");
        }
        PricePO pricePos = productDao.getPricePos(req.getProductId());
        if(pricePos!=null){
            if(!CollectionUtils.isEmpty(pricePos.getPriceInfos())&&(beginDate!=null)){
                Date finalEndDate = endDate;
                Date finalBeginDate = beginDate;
                List<PriceInfoPO> priceInfoList = pricePos.getPriceInfos().stream().filter(s -> (s.getSaleDate().compareTo(finalBeginDate)==0||s.getSaleDate().compareTo(finalBeginDate)==1)
                        &&(s.getSaleDate().compareTo(finalEndDate)==0||s.getSaleDate().compareTo(finalEndDate)==-1)).collect(Collectors.toList());
                if(CollectionUtils.isEmpty(priceInfoList)){
                    centerBookCheck.setMessage("库存不足，不能预订");
                    centerBookCheck.setErrorCode("003");
                    //todo 刷新库存逻辑

                    return centerBookCheck;
                }else{
                    priceInfoList.forEach(priceInfoPO->{
                        CenterBookCheckRes.ProductStock productStock = new CenterBookCheckRes.ProductStock();
                        productStock.setSaleDate(priceInfoPO.getSaleDate());
                        productStock.setStockCount(priceInfoPO.getStock());
                        productStockList.add(productStock);
                    });
                    centerBookCheck.setProductCount(productStockList);
                }
            }else{
                centerBookCheck.setMessage("库存不足，不能预订");
                centerBookCheck.setErrorCode("003");
                //todo 刷新库存逻辑

                return centerBookCheck;
            }
        }else{
            log.info("没有该类产品 productCode :{}",req.getProductId());
            centerBookCheck.setProductId(req.getProductId());
            centerBookCheck.setMessage("请检查输入产品编码，没有该类产品");
            centerBookCheck.setErrorCode("001");
        }
        //*************************以下是对接供应商校验逻辑***********************************
//        //供应商输出
//        YcfBookCheckRes ycfBookCheckRes = null;
//        //开始组装供应商请求参数
//        YcfBookCheckReq ycfBookCheckReq = new YcfBookCheckReq();
//        ycfBookCheckReq.setProductId(req.getProductId());
//        try {
//            ycfBookCheckReq.setBeginDate(DateUtils.parseTimeStringToDate(begin));
//            ycfBookCheckReq.setEndDate(DateUtils.parseTimeStringToDate(end));
//        } catch (ParseException e) {
//            log.error("");
//        }
//        try {
//            YcfBaseResult<YcfBookCheckRes> checkInfos = ycfOrderService.getCheckInfos(ycfBookCheckReq);
//            ycfBookCheckRes = checkInfos.getData();
//            //测试数据 start
//            String jsonString = "{\"data\":{\"productId\":\"16\",\"saleInfos\":[{\"date\":\"2016-06-14\",\"price\":99,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":2},{\"itemId\":\"321\",\"stock\":99}]},{\"date\":\"2016-06-15\",\"price\":98,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":2},{\"itemId\":\"321\",\"stock\":99}]},{\"date\":\"2016-06-16\",\"price\":97,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":10},{\"itemId\":\"321\",\"stock\":99}]},{\"date\":\"2016-06-17\",\"price\":96,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":0},{\"itemId\":\"321\",\"stock\":99}]},{\"date\":\"2016-06-18\",\"price\":95,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":2},{\"itemId\":\"321\",\"stock\":99}]}]},\"partnerId\":\"zx1000020160229\",\"success\":true,\"message\":null,\"statusCode\":200}";
//            YcfBaseResult ycfBaseResult = JSONObject.parseObject(jsonString,YcfBaseResult.class);
//            //测试数据  end
//            //供应商返回输入中台
//            if(ycfBookCheckRes!=null){
//                centerBookCheck.setProductId(req.getProductId());
//                centerBookCheck.setProductCount(10);
//            centerBookCheck = JSONObject.parseObject(JSONObject.toJSONString(ycfBaseResult.getData()), CenterBookCheckRes.CenterBookCheck.class);
//            }
//        }catch (Exception e){
//            centerBookCheck.setErrorCode("001");
//            centerBookCheck.setMessage("这是业务异常..");
//            log.error("ycfOrderService --> getNBCheckInfos rpc服务异常。。",e);
//        }
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
        if(this.getNBCheckInfos(bookCheckReq).getErrorCode() !=null){
            log.error("产品编号：{}，不能创建订单",req.getProductId());
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
        //封装中台创建订单返回结果
        CenterCreateOrderRes centerCreateOrderRes = new CenterCreateOrderRes();
        CenterCreateOrderRes.Supplier supplier = new CenterCreateOrderRes.Supplier();
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
        CenterCreateOrderRes.CreateOrderRes createOrderRes = createOrderConverter.convertSupplierResponseToResponse(ycfCreateOrderRes);
        //todo 通过查数据库封装中台结果集
        supplier.setCreateOrderObj(createOrderRes);
        supplier.setType(CentralUtils.getSupplierId(req.getProductId()));
        centerCreateOrderRes.setSupplier(supplier);
        return centerCreateOrderRes;
    }

    public CenterPayOrderRes getCenterPayOrder(PayOrderReq req) throws RuntimeException{

        //转换前端传参
        YcfPayOrderReq ycfPayOrderReq = payOrderConverter.convertRequestToSupplierRequest(req);
        //封装中台创建订单返回结果
        CenterPayOrderRes centerPayOrderrRes = new CenterPayOrderRes();
        CenterPayOrderRes.Supplier supplier = new CenterPayOrderRes.Supplier();
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
        CenterPayOrderRes.PayOrderRes payOrderRes = payOrderConverter.convertSupplierResponseToResponse(ycfPayOrderRes);
        supplier.setPayOrderObj(payOrderRes);
        supplier.setType(CentralUtils.getSupplierId(req.getProductCode()));
        centerPayOrderrRes.setSupplier(supplier);
        return centerPayOrderrRes;
    }

}
