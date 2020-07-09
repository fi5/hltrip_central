package com.huoli.trip.central.web.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSONObject;
import com.huoli.trip.central.web.converter.CancelOrderConverter;
import com.huoli.trip.central.web.converter.CreateOrderConverter;
import com.huoli.trip.central.web.converter.OrderInfoTranser;
import com.huoli.trip.central.web.converter.PayOrderConverter;
import com.huoli.trip.central.web.dao.ProductDao;
import com.huoli.trip.central.web.util.CentralUtils;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.constant.ChannelConstant;
import com.huoli.trip.common.entity.*;
import com.huoli.trip.common.util.DateTimeUtil;
import com.huoli.trip.common.vo.request.*;
import com.huoli.trip.common.vo.request.central.ProductPriceReq;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.central.ProductPriceDetialResult;
import com.huoli.trip.common.vo.response.order.*;
import com.huoli.trip.supplier.api.YcfOrderService;
import com.huoli.trip.supplier.self.yaochufa.vo.*;
import com.huoli.trip.supplier.self.yaochufa.vo.basevo.YcfBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
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
    public BaseResponse<CenterBookCheck> getCenterCheckInfos(BookCheckReq req) throws RuntimeException{
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
            //测试数据 start
            String jsonString = "{\"data\":{\"productId\":\"16\",\"saleInfos\":[{\"date\":\"2016-06-14\",\"price\":99,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":2},{\"itemId\":\"321\",\"stock\":99}]},{\"date\":\"2016-06-15\",\"price\":98,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":2},{\"itemId\":\"321\",\"stock\":99}]},{\"date\":\"2016-06-16\",\"price\":97,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":10},{\"itemId\":\"321\",\"stock\":99}]},{\"date\":\"2016-06-17\",\"price\":96,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":0},{\"itemId\":\"321\",\"stock\":99}]},{\"date\":\"2016-06-18\",\"price\":95,\"priceType\":0,\"totalStock\":2,\"stockList\":[{\"itemId\":\"123\",\"stock\":2},{\"itemId\":\"321\",\"stock\":99}]}]},\"partnerId\":\"zx1000020160229\",\"success\":true,\"message\":null,\"statusCode\":200}";
            YcfBaseResult ycfBaseResult = JSONObject.parseObject(jsonString,YcfBaseResult.class);
            ycfBookCheckRes = JSONObject.parseObject(JSONObject.toJSONString(ycfBaseResult.getData()), YcfBookCheckRes.class);
            //测试数据  end
            //供应商返回输入中台
            if(ycfBookCheckRes==null){
                log.error("预订前校验  供应商返回空对象 产品id:{}",req.getProductId());
                return BaseResponse.fail(CentralError.NO_RESULT_ERROR);
            }
            List<YcfBookSaleInfo> saleInfos = ycfBookCheckRes.getSaleInfos();
            //没有库存
            if(CollectionUtils.isEmpty(saleInfos)) {
                centerBookCheck.setMessage(CentralError.NO_STOCK_ERROR.getError());
                return centerBookCheck;
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
                    centerBookCheck.setMessage(CentralError.NOTENOUGH_STOCK_ERROR.getError());
                    centerBookCheck.setData(bookCheck);
                    log.info("传的产品份数大于库存剩余 产品编号：{}",req.getProductId());
                    return centerBookCheck;
                }
            }
        }catch (Exception e){
            log.error("ycfOrderService --> getNBCheckInfos rpc服务异常 :{}",e);
        }
//        //todo  结算价返回(按照份数来计算的  调用方法)
//        centerBookCheck.setSettlePrice();
        if(stockList.size()>0){
            bookCheck.setStock(stockList.get(0));
        }
        //todo  调用方法  计算销售价返回给前端
        bookCheck.setSalePrice(new BigDecimal(0));
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
            OrderDetailRep rep=new OrderDetailRep();
            rep.setOrderId(req.getOrderId());
            rep.setVochers(JSONObject.parseArray(JSONObject.toJSONString(data.getVochers()), OrderDetailRep.Voucher.class));
            return BaseResponse.success(rep);
        } catch (Exception e) {
            log.info("",e);
            return BaseResponse.fail(-101,vochers.getMessage(),null);
        }

    }

    public BaseResponse<CenterCreateOrderRes> getCenterCreateOrder(CreateOrderReq req) throws RuntimeException{
        //中台封装返回
        CenterCreateOrderRes createOrderRes = new CenterCreateOrderRes();
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
        //todo 总的结算价 调用方法获取(productid,beginDate,endDate,count)
        //总的结算价
        BigDecimal amount = new BigDecimal(0);
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
        ycfCreateOrderReq.setAmount(amount);
        ycfCreateOrderReq.setFoodDetail(ycfBookFoods);
        ycfCreateOrderReq.setPriceDetail(ycfPriceItemList);
        ycfCreateOrderReq.setRoomDetail(ycfBookRooms);
        ycfCreateOrderReq.setTicketDetail(ycfBookTickets);
        //供应商对象包装业务实体类
        YcfCreateOrderRes ycfCreateOrderRes = new YcfCreateOrderRes();
//        try {
//            YcfBaseResult<YcfCreateOrderRes> ycfOrder = ycfOrderService.createOrder(ycfCreateOrderReq);
//            ycfCreateOrderRes = ycfOrder.getData();
//        }catch (Exception e){
//            log.error("ycfOrderService --> getNBCreateOrder rpc服务异常 :{}",e);
//            throw new RuntimeException("ycfOrderService --> rpc服务异常");
//        }
        //测试数据 start
        String jsonString = "{\"data\":{\"orderStatus\":0,\"orderId\":\"1234567890\"},\"success\":true,\"message\":null,\"partnerId\":\"zx1000020160229\",\"statusCode\":200}";
        YcfBaseResult ycfBaseResult = JSONObject.parseObject(jsonString,YcfBaseResult.class);
        ycfCreateOrderRes = JSONObject.parseObject(JSONObject.toJSONString(ycfBaseResult.getData()), YcfCreateOrderRes.class);
        //测试数据  end
        createOrderRes = createOrderConverter.convertSupplierResponseToResponse(ycfCreateOrderRes);
        return new BaseResponse(0,true,"这是中台描述",createOrderRes);
    }

    public BaseResponse<CenterPayOrderRes> getCenterPayOrder(PayOrderReq req) throws RuntimeException{

        //封装中台创建订单返回结果
        CenterPayOrderRes payOrderRes = new CenterPayOrderRes();
        //转换前端传参
        YcfPayOrderReq ycfPayOrderReq = payOrderConverter.convertRequestToSupplierRequest(req);
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
        payOrderRes = payOrderConverter.convertSupplierResponseToResponse(ycfPayOrderRes);
        //组装本地订单号参数
        payOrderRes.setLocalOrderId(req.getPartnerOrderId());
        return new BaseResponse(0,true,"这是中台描述",payOrderRes);
    }

    public BaseResponse<CenterCancelOrderRes> getCenterCancelOrder(CancelOrderReq req) throws RuntimeException{
        //转换前端传参
        YcfCancelOrderReq ycfCancelOrderReq = cancelOrderConverter.convertRequestToSupplierRequest(req);
        //封装中台返回结果
        CenterCancelOrderRes cancelOrderRes = new CenterCancelOrderRes();
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
        cancelOrderRes = cancelOrderConverter.convertSupplierResponseToResponse(ycfCancelOrderRes);
        return new BaseResponse(0,true,"这是中台描述",cancelOrderRes);
    }

    public  BaseResponse<Boolean> payCheck(PayOrderReq req){
        //todo 支付前校验逻辑
        return BaseResponse.success(true);
    }

    public BaseResponse<CenterCancelOrderRes> getCenterApplyRefund(CancelOrderReq req) throws RuntimeException{
        //转换前端传参
        YcfCancelOrderReq ycfCancelOrderReq = cancelOrderConverter.convertRequestToSupplierRequest(req);
        //封装中台返回结果
        CenterCancelOrderRes cancelOrderRes = new CenterCancelOrderRes();
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
        cancelOrderRes = cancelOrderConverter.convertSupplierResponseToResponse(ycfCancelOrderRes);
        //todo 退款返回文案与取消订单不一样
        return new BaseResponse(0,true,"退款成功",cancelOrderRes);
    }


    public ProductPriceDetialResult getStockPrice(ProductPriceReq req){

        YcfGetPriceRequest stockPriceReq=new YcfGetPriceRequest();
        stockPriceReq.setProductID(req.getSupplierProductId());
        stockPriceReq.setPartnerProductID(req.getProductCode());
        stockPriceReq.setStartDate(req.getStartDate());
        stockPriceReq.setEndDate(req.getEndDate());
        try {
            final YcfBaseResult<YcfGetPriceResponse> stockPrice = ycfOrderService.getStockPrice(stockPriceReq);
            return  null;//TODo
        } catch (Exception e) {
            log.info("",e);
            return  null;
        }
    }
}
