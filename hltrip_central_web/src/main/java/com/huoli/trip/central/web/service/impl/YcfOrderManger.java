package com.huoli.trip.central.web.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSONObject;
import com.huoli.trip.central.api.ProductService;
import com.huoli.trip.central.web.converter.*;
import com.huoli.trip.central.web.dao.ProductDao;
import com.huoli.trip.central.web.util.CentralUtils;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.constant.ChannelConstant;
import com.huoli.trip.common.entity.*;
import com.huoli.trip.common.util.DateTimeUtil;
import com.huoli.trip.common.vo.request.*;
import com.huoli.trip.common.vo.request.central.PriceCalcRequest;
import com.huoli.trip.common.vo.request.central.ProductPriceReq;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.central.PriceCalcResult;
import com.huoli.trip.common.vo.response.order.*;
import com.huoli.trip.supplier.api.YcfOrderService;
import com.huoli.trip.supplier.api.YcfSyncService;
import com.huoli.trip.supplier.self.yaochufa.vo.*;
import com.huoli.trip.supplier.self.yaochufa.vo.basevo.YcfBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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
    @Reference(group = "hltrip")
    private YcfOrderService ycfOrderService;

    @Reference(group = "hltrip")
    private YcfSyncService ycfSynService;

    @Autowired
    private ProductDao productDao;
    @Autowired
    private ProductService productService;
    @Autowired
    private CreateOrderConverter createOrderConverter;
    @Autowired
    private PayOrderConverter payOrderConverter;
    @Autowired
    private CancelOrderConverter cancelOrderConverter;
    @Autowired
    private ApplyRefundConverter applyRefundConverter;
    public final static String CHANNEL= ChannelConstant.SUPPLIER_TYPE_YCF;
    public String getChannel(){
        return CHANNEL;
    }
    public String test() {
        System.out.println("ycf");
        return "ycf";
    }
    public BaseResponse<CenterBookCheck> getCenterCheckInfos(BookCheckReq req){
        //中台输出
        BaseResponse<CenterBookCheck> centerBookCheck = new BaseResponse<CenterBookCheck>();
        CenterBookCheck  bookCheck = new CenterBookCheck();
        //封装中台库存量
        List<Integer> stockList = new ArrayList<>();
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
        ycfBookCheckReq.setBeginDate(DateTimeUtil.parseDate(begin));
        ycfBookCheckReq.setEndDate(DateTimeUtil.parseDate(end));
        try {
            checkInfos = ycfOrderService.getCheckInfos(ycfBookCheckReq);
            ycfBookCheckRes = checkInfos.getData();
        }catch (Exception e){
            log.error("ycfOrderService --> getNBCheckInfos rpc服务异常");
            return new BaseResponse(1,false,"中台自定义描述");
        }
        if(ycfBookCheckRes == null){
            return new BaseResponse(1,false,checkInfos.getMessage());
        }
//            //测试数据 start
//            String jsonString = "{\"data\":{\"productId\":\"16\",\"saleInfos\":[{\"date\":\"2016-06-14\",\"price\":99,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":2},{\"itemId\":\"321\",\"stock\":99}]},{\"date\":\"2016-06-15\",\"price\":98,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":2},{\"itemId\":\"321\",\"stock\":99}]},{\"date\":\"2016-06-16\",\"price\":97,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":10},{\"itemId\":\"321\",\"stock\":99}]},{\"date\":\"2016-06-17\",\"price\":96,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":0},{\"itemId\":\"321\",\"stock\":99}]},{\"date\":\"2016-06-18\",\"price\":95,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":2},{\"itemId\":\"321\",\"stock\":99}]}]},\"partnerId\":\"zx1000020160229\",\"success\":true,\"message\":null,\"statusCode\":200}";
//            YcfBaseResult ycfBaseResult = JSONObject.parseObject(jsonString,YcfBaseResult.class);
//            ycfBookCheckRes = JSONObject.parseObject(JSONObject.toJSONString(ycfBaseResult.getData()), YcfBookCheckRes.class);
//            //测试数据  end
        //供应商返回输入中台
        if(ycfBookCheckRes==null){
            log.error("预订前校验  供应商返回空对象 产品id:{}",req.getProductId());
            return BaseResponse.fail(CentralError.NO_RESULT_ERROR);
        }
        List<YcfBookSaleInfo> saleInfos = ycfBookCheckRes.getSaleInfos();
        //没有库存
        if(CollectionUtils.isEmpty(saleInfos)) {
            return BaseResponse.fail(CentralError.NO_STOCK_ERROR);
        }
        saleInfos.forEach(ycfBookSaleInfo -> {
            stockList.add(ycfBookSaleInfo.getTotalStock());
        });
        if(stockList.size()>0){
            //库存数排序从小到大，如果多个库存量就取最小库存数返回去就得了呗
            Collections.sort(stockList);
//                if(!CollectionUtils.isEmpty(stockList)&&stockList.size()>1){
//                    int min = stockList.stream().filter(stock -> stock>0).min(Comparator.naturalOrder()).orElse(null);
//                }
            //证明传的产品份数大于供应商库存最小剩余
            if(req.getCount()>stockList.get(0)){
                bookCheck.setStock(stockList.get(0));
                log.info("传的产品份数大于库存剩余 产品编号：{}",req.getProductId());
                return new BaseResponse(1,false,CentralError.NOTENOUGH_STOCK_ERROR.getError(),bookCheck);
            }
        }
        //组装价格计算服务的请求
        PriceCalcRequest calcRequest = new PriceCalcRequest();
        calcRequest.setStartDate(DateTimeUtil.parseDate(begin));
        calcRequest.setEndDate(DateTimeUtil.parseDate(end));
        calcRequest.setProductCode(req.getProductId());
        calcRequest.setQuantity(req.getCount());
        BaseResponse<PriceCalcResult> priceCalcResultBaseResponse = null;
        try{
            priceCalcResultBaseResponse = productService.calcTotalPrice(calcRequest);
        }catch (Exception e){
            log.error("大兄弟额  你的价格计算服务 又挂了=.=  :{}",e);
            if(priceCalcResultBaseResponse!=null){
                log.error("大兄弟额  你的价格计算服务 又挂了=.=  :{}",priceCalcResultBaseResponse.getMessage());
            }
        }
        if(priceCalcResultBaseResponse!=null&&priceCalcResultBaseResponse.getData()!=null){
            //设置结算总价
            bookCheck.setSettlePrice(priceCalcResultBaseResponse.getData().getSettlesTotal());
            //销售总价
            bookCheck.setSalePrice(priceCalcResultBaseResponse.getData().getSalesTotal());
        }
        //设置库存
        if(stockList.size()>0){
            bookCheck.setStock(stockList.get(0));
        }
        return new BaseResponse(0,true,checkInfos.getMessage(),bookCheck);
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
//            rep.setVochers(JSONObject.parseArray(JSONObject.toJSONString(data.getVochers()), OrderDetailRep.Voucher.class));
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
            if(!vochers.getStatusCode().equals("200"))
                return BaseResponse.fail(9999,vochers.getMessage(),null);
            OrderDetailRep rep=new OrderDetailRep();
            rep.setOrderId(req.getOrderId());
            rep.setVochers(JSONObject.parseArray(JSONObject.toJSONString(data.getVochers()), OrderDetailRep.Voucher.class));
            return BaseResponse.success(rep);
        } catch (Exception e) {
            log.info("",e);
            return BaseResponse.fail(9999,vochers.getMessage(),null);
        }

    }

    public BaseResponse<CenterCreateOrderRes> getCenterCreateOrder(CreateOrderReq req){
        //中台封装返回
        CenterCreateOrderRes createOrderRes = null;
        //先校验是否可以预定
        BookCheckReq bookCheckReq = new BookCheckReq();
        bookCheckReq.setProductId(req.getProductId());
        bookCheckReq.setBeginDate(req.getBeginDate());
        bookCheckReq.setEndDate(req.getEndDate());
        bookCheckReq.setCount(req.getQunatity());
//        //校验可查询预订
//        CenterBookCheck checkInfo = this.getNBCheckInfos(bookCheckReq);
//        if(checkInfo.getStock()<1){
//            log.error("创建订单失败，预订前校验失败！产品编号：{}，不能创建订单",req.getProductId());
//            return centerCreateOrderRes;
//        }
        //转换客户端传来的参数
        YcfCreateOrderReq ycfCreateOrderReq = createOrderConverter.convertRequestToSupplierRequest(req);
        //以下 查产品数据库组装供应商需要的请求
        //获取中台价格日历
        PricePO pricePos = productDao.getPricePos(req.getProductId());
        //获取中台产品信息
        ProductPO productPO = productDao.getTripProductByCode(req.getProductId());
        //价格集合
        List<YcfPriceItem> ycfPriceItemList = new ArrayList<>();
        if(pricePos!=null){
            pricePos.getPriceInfos().forEach(price->{
                //价格对象
                YcfPriceItem ycfPriceItem = new YcfPriceItem();
                ycfPriceItem.setDate(DateTimeUtil.parseDate(req.getBeginDate(), DateTimeUtil.YYYYMMDD));
                ycfPriceItemList.add(ycfPriceItem);
            });
        }
        //本地餐饮
        FoodPO food = productPO.getFood();
        //本地房资源
        RoomPO room = productPO.getRoom();
        //本地票资源
        TicketPO ticket = productPO.getTicket();
        //**餐饮**
        List<YcfBookFood> ycfBookFoods = new ArrayList<>();
        List<FoodInfoPO> foods = food.getFoods();
        if(food!=null && !CollectionUtils.isEmpty(foods)){
            foods.forEach(f ->{
                YcfBookFood ycfBookFood = new YcfBookFood();
                ycfBookFood.setFoodId(f.getSupplierItemId());
                ycfBookFood.setCheckInDate(req.getBeginDate());
                ycfBookFoods.add(ycfBookFood);
            });
        }
        //**房资源组**
        List<YcfBookRoom> ycfBookRooms = new ArrayList<>();
        List<RoomInfoPO> rooms = room.getRooms();
        if(room!=null && !CollectionUtils.isEmpty(rooms)){
            rooms.forEach(roomInfoPO ->{
                YcfBookRoom ycfBookRoom = new YcfBookRoom();
                ycfBookRoom.setCheckInDate(req.getBeginDate());
                ycfBookRoom.setCheckOutDate(req.getEndDate());
                ycfBookRooms.add(ycfBookRoom);
            });
        }
        //**门票资源组**
        List<YcfBookTicket> ycfBookTickets = new ArrayList<>();
        List<TicketInfoPO> tickets = ticket.getTickets();
        if(ticket!=null && !CollectionUtils.isEmpty(tickets)){
            tickets.forEach(ticketInfoPO ->{
                YcfBookTicket ycfBookTicket = new YcfBookTicket();
                ycfBookTicket.setCheckInDate(req.getBeginDate());
                ycfBookTicket.setTicketId(ticketInfoPO.getSupplierItemId());
                ycfBookTickets.add(ycfBookTicket);
            });
        }
        //组装价格计算服务的请求
        PriceCalcRequest calcRequest = new PriceCalcRequest();
        calcRequest.setStartDate(DateTimeUtil.parseDate(req.getBeginDate()));
        calcRequest.setEndDate(DateTimeUtil.parseDate(req.getEndDate()));
        calcRequest.setProductCode(req.getProductId());
        calcRequest.setQuantity(req.getQunatity());
        BaseResponse<PriceCalcResult>  priceCalcResultBaseResponse = null;
        try{
            priceCalcResultBaseResponse = productService.calcTotalPrice(calcRequest);
        }catch (Exception e){
            log.error("大兄弟额  你的价格计算服务 又挂了=.=  :{}",e);
            if(priceCalcResultBaseResponse!=null){
                log.info("大兄弟额  你的价格计算服务 又挂了=.=  :{}",priceCalcResultBaseResponse.getMessage());
            }
        }
        if(priceCalcResultBaseResponse!=null && priceCalcResultBaseResponse.getData()!=null){
            //总的结算价
            ycfCreateOrderReq.setAmount(priceCalcResultBaseResponse.getData().getSettlesTotal());
        }
        ycfCreateOrderReq.setFoodDetail(ycfBookFoods);
        ycfCreateOrderReq.setPriceDetail(ycfPriceItemList);
        ycfCreateOrderReq.setRoomDetail(ycfBookRooms);
        ycfCreateOrderReq.setTicketDetail(ycfBookTickets);
        //供应商对象包装业务实体类
        YcfBaseResult<YcfCreateOrderRes> ycfOrder = new YcfBaseResult<>();
        YcfCreateOrderRes ycfCreateOrderRes = null;
        try {
            ycfOrder = ycfOrderService.createOrder(ycfCreateOrderReq);
            ycfCreateOrderRes = ycfOrder.getData();
        }catch (Exception e){
            log.error("ycfOrderService --> getNBCreateOrder rpc服务异常 :{}",e);
            return new BaseResponse(1,false,"中台自定义描述");
        }
        if(ycfCreateOrderRes == null){
            return new BaseResponse(1,false,ycfOrder.getMessage());
        }
//        //测试数据 start
//        String jsonString = "{\"data\":{\"orderStatus\":0,\"orderId\":\"1234567890\"},\"success\":true,\"message\":null,\"partnerId\":\"zx1000020160229\",\"statusCode\":200}";
//        YcfBaseResult ycfBaseResult = JSONObject.parseObject(jsonString,YcfBaseResult.class);
//        ycfCreateOrderRes = JSONObject.parseObject(JSONObject.toJSONString(ycfBaseResult.getData()), YcfCreateOrderRes.class);
//        //测试数据  end
        createOrderRes = createOrderConverter.convertSupplierResponseToResponse(ycfCreateOrderRes);
        return new BaseResponse(0,true,null,createOrderRes);
    }

    public BaseResponse<CenterPayOrderRes> getCenterPayOrder(PayOrderReq req){
        //封装中台创建订单返回结果
        CenterPayOrderRes payOrderRes = new CenterPayOrderRes();
        //转换前端传参
        YcfPayOrderReq ycfPayOrderReq = payOrderConverter.convertRequestToSupplierRequest(req);
        //供应商输出
        YcfBaseResult<YcfPayOrderRes> ycfPayOrder = new YcfBaseResult<>();
        YcfPayOrderRes ycfPayOrderRes = null;
        try {
            ycfPayOrder = ycfOrderService.payOrder(ycfPayOrderReq);
            ycfPayOrderRes = ycfPayOrder.getData();
        }catch (Exception e){
            log.error("ycfOrderService --> getCenterPayOrder rpc服务异常 :{}",e);
            return new BaseResponse(1,false,"中台自定义描述");
        }
        if(ycfPayOrderRes == null){
            return new BaseResponse(1,false,ycfPayOrder.getMessage());
        }
//        //测试数据 start
//        String jsonString = "{\"data\":{\"orderStatus\":10,\"orderId\":\"1234567890\"},\"success\":true,\"message\":null,\"partnerId\":\"zx1000020160229\",\"statusCode\":200}";
//        YcfBaseResult ycfBaseResult = JSONObject.parseObject(jsonString,YcfBaseResult.class);
//        ycfPayOrderRes = JSONObject.parseObject(JSONObject.toJSONString(ycfBaseResult.getData()), YcfPayOrderRes.class);
//        //测试数据  end
        //封装中台返回结果
        payOrderRes = payOrderConverter.convertSupplierResponseToResponse(ycfPayOrderRes);
        //组装本地订单号参数
        if(payOrderRes != null){
            payOrderRes.setLocalOrderId(req.getPartnerOrderId());
        }
        return new BaseResponse(0,true,null,payOrderRes);
    }

    public BaseResponse<CenterCancelOrderRes> getCenterCancelOrder(CancelOrderReq req) throws RuntimeException{
        //转换前端传参
        YcfCancelOrderReq ycfCancelOrderReq = cancelOrderConverter.convertRequestToSupplierRequest(req);
        //封装中台返回结果
        CenterCancelOrderRes cancelOrderRes = new CenterCancelOrderRes();
        //供应商输出
        YcfBaseResult<YcfCancelOrderRes> ycfBaseResult = new YcfBaseResult<>();
        YcfCancelOrderRes ycfCancelOrderRes = new YcfCancelOrderRes();
        try {
            ycfBaseResult = ycfOrderService.cancelOrder(ycfCancelOrderReq);
            ycfCancelOrderRes = ycfBaseResult.getData();
        }catch (Exception e){
            log.error("ycfOrderService --> getCenterPayOrder rpc服务异常 ：{}",e);
            return new BaseResponse(1,false,"中台自定义描述");
        }
        if(ycfCancelOrderRes == null){
            return new BaseResponse(1,false,ycfBaseResult.getMessage());
        }
//        //测试数据 start
//        String jsonString = "{\"data\":{\"orderStatus\":null,\"orderId\":\"45775553335\",\"async\":1},\"success\":true,\"message\":null,\"partnerId\":\"zx1000020160229\",\"statusCode\":200}";
//        YcfBaseResult ycfBaseResult = JSONObject.parseObject(jsonString,YcfBaseResult.class);
//        ycfCancelOrderRes = JSONObject.parseObject(JSONObject.toJSONString(ycfBaseResult.getData()), YcfCancelOrderRes.class);
//        //测试数据  end
        //组装中台返回结果
        cancelOrderRes = cancelOrderConverter.convertSupplierResponseToResponse(ycfCancelOrderRes);
        return new BaseResponse(0,true,null,cancelOrderRes);
    }

    public  BaseResponse<CenterPayCheckRes> payCheck(PayOrderReq req){
        //todo 支付前校验逻辑
        CenterPayCheckRes result = new CenterPayCheckRes();
        result.setResult(true);
        return BaseResponse.success(result);
    }

    public BaseResponse<CenterCancelOrderRes> getCenterApplyRefund(CancelOrderReq req){
        //转换前端传参
        YcfCancelOrderReq ycfCancelOrderReq = applyRefundConverter.convertRequestToSupplierRequest(req);
        //封装中台返回结果
        CenterCancelOrderRes applyRefundRes = new CenterCancelOrderRes();
        //供应商输出
        YcfBaseResult<YcfCancelOrderRes> ycfBaseResult = new YcfBaseResult<>();
        YcfCancelOrderRes ycfCancelOrderRes = new YcfCancelOrderRes();
        try {
            ycfBaseResult = ycfOrderService.cancelOrder(ycfCancelOrderReq);
            ycfCancelOrderRes = ycfBaseResult.getData();
        }catch (Exception e){
            log.error("ycfOrderService --> getCenterPayOrder rpc服务异常 ：{}",e);
            return new BaseResponse(1,false,"中台自定义描述");
        }
        if(ycfCancelOrderRes == null){
            return new BaseResponse(1,false,ycfBaseResult.getMessage());
        }
//        //测试数据 start
//        String jsonString = "{\"data\":{\"orderStatus\":null,\"orderId\":\"45775553335\",\"async\":1},\"success\":true,\"message\":null,\"partnerId\":\"zx1000020160229\",\"statusCode\":200}";
//        YcfBaseResult ycfBaseResult = JSONObject.parseObject(jsonString,YcfBaseResult.class);
//        ycfCancelOrderRes = JSONObject.parseObject(JSONObject.toJSONString(ycfBaseResult.getData()), YcfCancelOrderRes.class);
//        //测试数据  end
        //组装中台返回结果
        applyRefundRes = applyRefundConverter.convertSupplierResponseToResponse(ycfCancelOrderRes);
        return new BaseResponse(0,true,null,applyRefundRes);
    }


    public void refreshStockPrice(ProductPriceReq req){

        try {
            YcfGetPriceRequest stockPriceReq=new YcfGetPriceRequest();
            stockPriceReq.setProductID(req.getSupplierProductId());
            stockPriceReq.setPartnerProductID(req.getProductCode());
            stockPriceReq.setStartDate(req.getStartDate());
            stockPriceReq.setEndDate(req.getEndDate());
            ycfSynService.getPrice(stockPriceReq);

        } catch (Exception e) {
            log.info("",e);
        }
    }
}
