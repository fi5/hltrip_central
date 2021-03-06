package com.huoli.trip.central.web.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.huoli.trip.central.api.ProductService;
import com.huoli.trip.central.web.converter.*;
import com.huoli.trip.central.web.dao.HotelScenicDao;
import com.huoli.trip.central.web.dao.ProductDao;
import com.huoli.trip.central.web.dao.ScenicSpotDao;
import com.huoli.trip.central.web.util.CentralUtils;
import com.huoli.trip.central.web.util.TraceIdUtils;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.constant.ChannelConstant;
import com.huoli.trip.common.constant.OrderStatus;
import com.huoli.trip.common.entity.*;
import com.huoli.trip.common.entity.mpo.hotelScenicSpot.HotelScenicSpotPriceStock;
import com.huoli.trip.common.entity.mpo.hotelScenicSpot.HotelScenicSpotProductBackupMPO;
import com.huoli.trip.common.entity.mpo.hotelScenicSpot.HotelScenicSpotProductMPO;
import com.huoli.trip.common.entity.mpo.hotelScenicSpot.HotelScenicSpotProductSetMealMPO;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.ScenicSpotProductBackupMPO;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.ScenicSpotProductMPO;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.ScenicSpotProductPriceMPO;
import com.huoli.trip.common.exception.HlCentralException;
import com.huoli.trip.common.util.DateTimeUtil;
import com.huoli.trip.common.vo.request.*;
import com.huoli.trip.common.vo.request.central.PriceCalcRequest;
import com.huoli.trip.common.vo.request.central.ProductPriceReq;
import com.huoli.trip.common.vo.request.v2.HotelScenicSetMealRequest;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.central.PriceCalcResult;
import com.huoli.trip.common.vo.response.order.*;
import com.huoli.trip.supplier.api.YcfOrderService;
import com.huoli.trip.supplier.api.YcfSyncService;
import com.huoli.trip.supplier.self.yaochufa.vo.*;
import com.huoli.trip.supplier.self.yaochufa.vo.basevo.YcfBaseResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
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
    @Reference(timeout = 10000,group = "hltrip", check = false)
    private YcfOrderService ycfOrderService;

    @Reference(timeout = 10000,group = "hltrip", check = false)
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
    @Autowired
    private ScenicSpotDao scenicSpotDao;
    @Autowired
    private HotelScenicDao hotelScenicDao;
    public final static String CHANNEL= ChannelConstant.SUPPLIER_TYPE_YCF;
    public String getChannel(){
        return CHANNEL;
    }
    public String test() {
        System.out.println("ycf");
        return "ycf";
    }
    public BaseResponse<CenterBookCheck> getCenterCheckInfos(BookCheckReq req){
        String begin = req.getBeginDate();
        String end = req.getEndDate();
        //没传结束时间这样处理
        if(StringUtils.isBlank(end)){
            end = begin;
        }
        //开始组装供应商请求参数
        YcfBookCheckReq ycfBookCheckReq = new YcfBookCheckReq();
        //转供应商productId
        if(StringUtils.isBlank(req.getCategory())){
            ycfBookCheckReq.setProductId(CentralUtils.getSupplierId(req.getProductId()));
        } else {
            switch (req.getCategory()){
                case "d_ss_ticket":
                    ScenicSpotProductMPO scenicSpotProductMPO = scenicSpotDao.querySpotProductById(req.getProductId(), null);
                    ycfBookCheckReq.setProductId(scenicSpotProductMPO.getSupplierProductId());
                    break;
                case "hotel_scenicSpot":
                    HotelScenicSpotProductMPO productMPO = hotelScenicDao.queryHotelScenicProductMpoById(req.getProductId(), null);
                    ycfBookCheckReq.setProductId(productMPO.getSupplierProductId());
                    break;
            }
        }
        ycfBookCheckReq.setBeginDate(begin);
        ycfBookCheckReq.setEndDate(end);
        /**
         * 开始日期大于结束日期
         */
        /*if(DateTimeUtil.parseDate(begin).after(DateTimeUtil.parseDate(begin))){
            log.error("预订前校验 开始日期大于结束日期 错误 产品编号：{}",req.getProductId());
            return BaseResponse.fail(CentralError.ERROR_DATE_ORDER_1);
        }*/
        /**
         * 时间跨度大于90天
         */
      /*  if(this.isOutTime(DateTimeUtil.parseDate(begin),DateTimeUtil.parseDate(begin))){
            log.error("预订前校验 时间跨度大于90天 错误 产品编号：{}",req.getProductId());
            return BaseResponse.fail(CentralError.ERROR_DATE_ORDER_2);
        }*/
        //供应商输出数据包
        YcfBookCheckRes ycfBookCheckRes = null;
        String traceId = req.getTraceId();
        if(StringUtils.isEmpty(traceId)){
            traceId = TraceIdUtils.getTraceId();
        }
        ycfBookCheckReq.setTraceId(traceId);
        try {
            //供应商输出
            YcfBaseResult<YcfBookCheckRes> checkInfos = ycfOrderService.getCheckInfos(ycfBookCheckReq);
            if(checkInfos!=null&&checkInfos.getStatusCode()==200){
                ycfBookCheckRes = checkInfos.getData();
                if(ycfBookCheckRes == null){
                    log.error("预订前校验  供应商返回空对象 产品id:{}  供应商异常描述 ：{},【请求供应商json】 :{}",req.getProductId(),checkInfos.getMessage(), JSONObject.toJSONString(ycfBookCheckReq));
                    return SupplierErrorMsgTransfer.buildMsg(checkInfos.getMessage());//异常消息以供应商返回的
                }
            }else{
                return BaseResponse.fail(CentralError.ERROR_SUPPLIER_BOOK_CHECK_ORDER);
            }
        }catch (HlCentralException e){
            log.error("ycfOrderService --> getNBCheckInfos rpc服务异常 ：{}",e);
            return BaseResponse.fail(CentralError.ERROR_SUPPLIER_BOOK_CHECK_ORDER);
        }
        //中台输出
        CenterBookCheck  bookCheck = new CenterBookCheck();
        //供应商返回输入中台
        List<YcfBookSaleInfo> saleInfos = ycfBookCheckRes.getSaleInfos();
        //封装中台库存量
        List<Integer> stockList = new ArrayList<>();
        //没有库存
        if(CollectionUtils.isEmpty(saleInfos)) {
            log.error("预订前校验  供应商返回空的saleInfos 产品id:{}  ",req.getProductId());
            return BaseResponse.fail(CentralError.NO_STOCK_ERROR);
        }
        saleInfos.forEach(ycfBookSaleInfo -> {
            stockList.add(ycfBookSaleInfo.getTotalStock());
        });
        //库存数排序从小到大，顾架说如果有多个库存量就取个最小的库存数返回去就得了呗
        if(stockList.size()>0){
            if(stockList.size()>1){
                Collections.sort(stockList);
            }
            //证明传的产品份数大于供应商库存最小剩余
            if(req.getCount()>stockList.get(0)){
                bookCheck.setStock(stockList.get(0));
                log.info("传的产品份数大于库存剩余 产品编号：{}",req.getProductId());
                return BaseResponse.withFail(CentralError.NOTENOUGH_STOCK_ERROR,bookCheck);
            }
        }
        //组装价格计算服务的请求
        PriceCalcRequest calcRequest = new PriceCalcRequest();
        calcRequest.setStartDate(DateTimeUtil.parseDate(begin));
        calcRequest.setEndDate(DateTimeUtil.parseDate(end));
        calcRequest.setProductCode(req.getProductId());
        calcRequest.setQuantity(req.getCount());
        //2021-06-02
        calcRequest.setChannelCode(req.getChannelCode());
        calcRequest.setFrom(req.getFrom());
        calcRequest.setPackageCode(req.getPackageId());
        PriceCalcResult priceCalcResult = null;
        calcRequest.setTraceId(traceId);
        calcRequest.setSource(req.getSource());
        try{
            BaseResponse<PriceCalcResult> priceCalcResultBaseResponse;
            if(StringUtils.isBlank(req.getCategory())){
                priceCalcResultBaseResponse = productService.calcTotalPrice(calcRequest);
            } else {
                priceCalcResultBaseResponse = productService.calcTotalPriceV2(calcRequest);
            }

            priceCalcResult = priceCalcResultBaseResponse.getData();
            //没有价格直接抛异常
            if(priceCalcResultBaseResponse.getCode()!=0||priceCalcResult==null){
                return BaseResponse.fail(CentralError.PRICE_CALC_PRICE_NOT_FOUND_ERROR);
            }
        }catch (HlCentralException e){
            log.error("大兄弟额  你的价格计算服务 又挂了=.=  :{}",e);
            return BaseResponse.fail(CentralError.PRICE_CALC_PRICE_NOT_FOUND_ERROR);
        }
        //设置结算总价
        bookCheck.setSettlePrice(priceCalcResult.getSettlesTotal());
        //销售总价
        bookCheck.setSalePrice(priceCalcResult.getSalesTotal());
        //设置库存
        if(stockList.size()>0){
            bookCheck.setStock(stockList.get(0));
        }
        return BaseResponse.success(bookCheck);
    }

   public BaseResponse<OrderDetailRep> getOrderDetail(OrderOperReq req){
        BaseOrderRequest baseOrderRequest = new BaseOrderRequest();
        baseOrderRequest.setOrderId(req.getOrderId());
        baseOrderRequest.setTraceId(req.getTraceId());
       final YcfBaseResult<YcfOrderStatusResult> order = ycfOrderService.getOrder(baseOrderRequest);
       if(order==null)
           return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
       try {
            log.info("拿到的订单数据:"+JSONObject.toJSONString(order));
            final YcfOrderStatusResult data = order.getData();
            //如果数据为空,直接返回错
            if(order.getStatusCode()!=200 || !order.getSuccess())
                return BaseResponse.fail(CentralError.ERROR_NO_ORDER);//异常消息以供应商返回的
            OrderDetailRep rep=new OrderDetailRep();
            rep.setOrderId(data.getOrderId());
            //转换成consumer统一的订单状态
            rep.setOrderStatus(OrderInfoTranser.genCommonOrderStatus(data.getOrderStatus(),1));
            rep.setVochers(JSONObject.parseArray(JSONObject.toJSONString(data.getVochers()), OrderDetailRep.Voucher.class));
            return BaseResponse.success(rep);
        } catch (Exception e) {
            log.error("YCFgetOrderDetail报错:"+JSONObject.toJSONString(req),e);
            return BaseResponse.fail(9999,order.getMessage(),null);
        }

    }

    public BaseResponse<OrderDetailRep> getVochers(OrderOperReq req){

        try {
            BaseOrderRequest baseOrderRequest = new BaseOrderRequest();
            baseOrderRequest.setOrderId(req.getOrderId());
            baseOrderRequest.setTraceId(req.getTraceId());
            final YcfBaseResult<YcfVouchersResult> vochers = ycfOrderService.getVochers(baseOrderRequest);
            if(null==vochers)
                return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
            log.info("拿到的数据:"+JSONObject.toJSONString(vochers));
            final YcfVouchersResult data = vochers.getData();
            if(vochers.getStatusCode()!=200 || !vochers.getSuccess())
                return BaseResponse.fail(OrderInfoTranser.findCentralError(vochers.getMessage()));//异常消息以供应商返回的
            OrderDetailRep rep=new OrderDetailRep();
            rep.setOrderId(req.getOrderId());
            rep.setVochers(JSONObject.parseArray(JSONObject.toJSONString(data.getVochers()), OrderDetailRep.Voucher.class));
            return BaseResponse.success(rep);
        } catch (Exception e) {
            log.error("YCFgetVochers报错:"+JSONObject.toJSONString(req),e);
            return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
        }

    }

    public BaseResponse<CenterCreateOrderRes> getCenterCreateOrder(CreateOrderReq req){
        //中台封装返回
        CenterCreateOrderRes createOrderRes = null;
        String begin = req.getBeginDate();
        String end = req.getEndDate();
        //没传结束时间这样处理
        if(StringUtils.isBlank(end)){
            end = begin;
        }
        //先校验是否可以预定
        BookCheckReq bookCheckReq = new BookCheckReq();
        //转供应商productId
        if(StringUtils.isBlank(req.getCategory())){
            bookCheckReq.setProductId(CentralUtils.getSupplierId(req.getProductId()));
        } else {
            switch (req.getCategory()){
                case "d_ss_ticket":
                    ScenicSpotProductMPO scenicSpotProductMPO = scenicSpotDao.querySpotProductById(req.getProductId(), null);
                    bookCheckReq.setProductId(scenicSpotProductMPO.getSupplierProductId());
                    break;
                case "hotel_scenicSpot":
                    HotelScenicSpotProductMPO productMPO = hotelScenicDao.queryHotelScenicProductMpoById(req.getProductId(), null);
                    bookCheckReq.setProductId(productMPO.getSupplierProductId());
                    break;
            }
        }
        bookCheckReq.setBeginDate(begin);
        bookCheckReq.setEndDate(end);
        bookCheckReq.setCount(req.getQunatity());
        String traceId = req.getTraceId();
        if(StringUtils.isEmpty(traceId)){
            traceId = TraceIdUtils.getTraceId();
        }
        bookCheckReq.setTraceId(traceId);
        //校验可查询预订
        BaseResponse<CenterBookCheck> centerCheckInfos = this.getCenterCheckInfos(bookCheckReq);
        if(centerCheckInfos.getCode() != 0){
            log.error("创建订单失败，预订前校验失败！产品编号：{}，不能创建订单",req.getProductId());
            return BaseResponse.fail(CentralError.ERROR_BOOKBEFORE_ORDER);
        }
        //订单的份数小于或等于0
        if(req.getQunatity()<=0){
            return BaseResponse.fail(CentralError.PRICE_CALC_QUANTITY_LIMIT_ERROR);
        }
        //*********************************************组装供应商请求开始**********************************************
        //**供应商餐饮**
        List<YcfBookFood> ycfBookFoods = new ArrayList<>();
        //**供应商房资源组**
        List<YcfBookRoom> ycfBookRooms = new ArrayList<>();
        //**供应商门票资源组**
        List<YcfBookTicket> ycfBookTickets = new ArrayList<>();
        //供应商价格集合
        List<YcfPriceItem> ycfPriceItemList = new ArrayList<>();
        //转换客户端传来的参数
        YcfCreateOrderReq ycfCreateOrderReq = createOrderConverter.convertRequestToSupplierRequest(req);
            if(StringUtils.isBlank(req.getCategory())){
            //获取中台产品信息
            ProductPO productPO = productDao.getTripProductByCode(req.getProductId());
            //本地餐饮
            FoodPO food = productPO.getFood();
            List<FoodInfoPO> foods = food.getFoods();
            //本地房资源
            RoomPO room = productPO.getRoom();
            List<RoomInfoPO> rooms = room.getRooms();
            //本地票资源
            TicketPO ticket = productPO.getTicket();
            List<TicketInfoPO> tickets = ticket.getTickets();

                //**供应商餐饮**
                if(!CollectionUtils.isEmpty(foods)){
                    foods.forEach(f ->{
                        YcfBookFood ycfBookFood = new YcfBookFood();
                        ycfBookFood.setFoodId(f.getSupplierResourceId());
                        ycfBookFood.setCheckInDate(req.getBeginDate());
                        ycfBookFoods.add(ycfBookFood);
                    });
                }
                //**供应商房资源组**
                if(!CollectionUtils.isEmpty(rooms)){
                    rooms.forEach(roomInfoPO ->{
                        YcfBookRoom ycfBookRoom = new YcfBookRoom();
                        ycfBookRoom.setCheckInDate(req.getBeginDate());
                        ycfBookRoom.setCheckOutDate(req.getEndDate());
                        ycfBookRoom.setRoomId(roomInfoPO.getSupplierResourceId());
                        ycfBookRooms.add(ycfBookRoom);
                    });
                }
                //**供应商门票资源组**
                if(!CollectionUtils.isEmpty(tickets)){
                    tickets.forEach(ticketInfoPO ->{
                        YcfBookTicket ycfBookTicket = new YcfBookTicket();
                        ycfBookTicket.setCheckInDate(req.getBeginDate());
                        ycfBookTicket.setTicketId(ticketInfoPO.getSupplierResourceId());
                        ycfBookTickets.add(ycfBookTicket);
                    });
                }

            //获取中台价格日历
            PricePO pricePos = productDao.getPricePos(req.getProductId());
            if(pricePos!=null&&!CollectionUtils.isEmpty(pricePos.getPriceInfos())){
                //取时间范围内的价格集合,以酒店入住范围为基准，如果没有酒店  日期就取小产品单元的使用时间
                List<PriceInfoPO> priceInfoPOS = null;
                if(!CollectionUtils.isEmpty(ycfBookRooms)&&ycfBookRooms.size()>0){
                    String finalEnd = end;
                    priceInfoPOS = pricePos.getPriceInfos().stream().filter(priceInfoPO -> DateTimeUtil.trancateToDate(priceInfoPO.getSaleDate()).getTime()>=DateTimeUtil.parseDate(begin,DateTimeUtil.YYYYMMDD).getTime()
                            && DateTimeUtil.trancateToDate(priceInfoPO.getSaleDate()).getTime()<=DateTimeUtil.parseDate(finalEnd,DateTimeUtil.YYYYMMDD).getTime()).collect(Collectors.toList());
                }else{
                    priceInfoPOS = pricePos.getPriceInfos().stream().filter(priceInfoPO -> DateTimeUtil.trancateToDate(priceInfoPO.getSaleDate()).getTime()==DateTimeUtil.parseDate(begin,DateTimeUtil.YYYYMMDD).getTime()).collect(Collectors.toList());
                }
                if(!CollectionUtils.isEmpty(priceInfoPOS)){
                    for (PriceInfoPO price : priceInfoPOS) {
                        //价格对象
                        YcfPriceItem ycfPriceItem = new YcfPriceItem();
                        //YYYYMMDD
                        ycfPriceItem.setDate(DateTimeUtil.trancateToDate(price.getSaleDate()));
                        ycfPriceItem.setPrice(price.getSettlePrice());
                        ycfPriceItemList.add(ycfPriceItem);
                    };
                }
            }else{
                log.error("创建订单 产品数据库价格日历返回数据空 产品code: {}",req.getProductId());
            }
        } else {
            List<YcfResourceFood> foods = new ArrayList<>();
            List<YcfResourceTicket> tickets = new ArrayList<>();
            List<YcfResourceRoom> rooms = new ArrayList<>();
            YcfProduct ycfProduct = null;
            switch (req.getCategory()){
                case "d_ss_ticket":
                    ScenicSpotProductBackupMPO backupMPO = scenicSpotDao.queryBackInfoByProductId(req.getProductId());
                    if(StringUtils.isNotBlank(backupMPO.getOriginContent())){
                        ycfProduct = JSONObject.parseObject(backupMPO.getOriginContent(), YcfProduct.class);
                    }
                    ScenicSpotProductPriceMPO scenicSpotProductPriceMPO = scenicSpotDao.querySpotProductPriceById(req.getPackageId());
                    if(scenicSpotProductPriceMPO != null){
                        //价格对象
                        YcfPriceItem ycfPriceItem = new YcfPriceItem();
                        //YYYYMMDD
                        ycfPriceItem.setDate(DateTimeUtil.parseDate(scenicSpotProductPriceMPO.getStartDate()));
                        ycfPriceItem.setPrice(scenicSpotProductPriceMPO.getSettlementPrice());
                        ycfPriceItemList = Arrays.asList(ycfPriceItem);
                    }
                    break;
                case "hotel_scenicSpot":
                    HotelScenicSpotProductBackupMPO hotelBackUp = hotelScenicDao.queryBackInfoByProductId(req.getProductId());
                    if (StringUtils.isNotBlank(hotelBackUp.getOriginContent())) {
                        ycfProduct = JSONObject.parseObject(hotelBackUp.getOriginContent(), YcfProduct.class);
                    }
                    HotelScenicSetMealRequest request = new HotelScenicSetMealRequest();
                    request.setProductId(req.getProductId());
                    request.setPackageId(req.getPackageId());
                    Date startDate = new Date();
                    if(StringUtils.isNotBlank(req.getBeginDate())){
                        Date reqStartDate = DateTimeUtil.parse(req.getBeginDate(), DateTimeUtil.YYYYMMDD);
                        if(DateTimeUtil.getDateDiffDays(reqStartDate, startDate) > 0) {
                            startDate = reqStartDate;
                        }
                    }
                    Date endDate = null;
                    if(StringUtils.isNotBlank(req.getEndDate())){
                        Date reqEndDate = DateTimeUtil.parse(req.getEndDate(), DateTimeUtil.YYYYMMDD);
                        if(DateTimeUtil.getDateDiffDays(reqEndDate, startDate)> 0) {
                            endDate = reqEndDate;
                        }
                    }
                    HotelScenicSpotProductSetMealMPO setMealMPO = hotelScenicDao.queryHotelScenicSetMealById(request);
                    List<HotelScenicSpotPriceStock> priceStocks = setMealMPO.getPriceStocks();
                    for (HotelScenicSpotPriceStock priceStock : priceStocks) {
                        Date calendarDate = DateTimeUtil.parse(priceStock.getDate(), DateTimeUtil.YYYYMMDD);
                        if(DateTimeUtil.getDateDiffDays(calendarDate, startDate) < 0 || DateTimeUtil.getDateDiffDays(calendarDate, endDate) > 0){
                            continue;
                        }
                        //价格对象
                        YcfPriceItem ycfPriceItem = new YcfPriceItem();
                        //YYYYMMDD
                        ycfPriceItem.setDate(DateTimeUtil.parseDate(priceStock.getDate()));
                        ycfPriceItem.setPrice(priceStock.getAdtPrice());
                        ycfPriceItemList.add(ycfPriceItem);
                    }
                    break;
            }
            if(ycfProduct != null){
                foods = ycfProduct.getFoodList();
                tickets = ycfProduct.getTicketList();
                rooms = ycfProduct.getRoomList();
            }
            //传入参数没有包含任何资源(房，票，餐)
            if(CollectionUtils.isEmpty(foods)
                    && CollectionUtils.isEmpty(rooms)
                    && CollectionUtils.isEmpty(tickets)){
                log.error("下单传参至供应商错误,传入参数没有包含任何资源(房，票，餐) 产品编号：{}",req.getProductId());
                return BaseResponse.fail(CentralError.ERROR_ORDER);
            }
            //**供应商餐饮**
            if(!CollectionUtils.isEmpty(foods)){
                foods.forEach(f ->{
                    YcfBookFood ycfBookFood = new YcfBookFood();
                    ycfBookFood.setFoodId(f.getFoodId());
                    ycfBookFood.setCheckInDate(req.getBeginDate());
                    ycfBookFoods.add(ycfBookFood);
                });
            }
            //**供应商房资源组**
            if(!CollectionUtils.isEmpty(rooms)){
                rooms.forEach(roomInfoPO ->{
                    YcfBookRoom ycfBookRoom = new YcfBookRoom();
                    ycfBookRoom.setCheckInDate(req.getBeginDate());
                    ycfBookRoom.setCheckOutDate(req.getEndDate());
                    ycfBookRoom.setRoomId(roomInfoPO.getRoomId());
                    ycfBookRooms.add(ycfBookRoom);
                });
            }
            //**供应商门票资源组**
            if(!CollectionUtils.isEmpty(tickets)){
                tickets.forEach(ticketInfoPO ->{
                    YcfBookTicket ycfBookTicket = new YcfBookTicket();
                    ycfBookTicket.setCheckInDate(req.getBeginDate());
                    ycfBookTicket.setTicketId(ticketInfoPO.getTicketId());
                    ycfBookTickets.add(ycfBookTicket);
                });
            }
        }

        //组装价格计算服务的请求
        PriceCalcRequest calcRequest = new PriceCalcRequest();
        calcRequest.setStartDate(DateTimeUtil.parseDate(begin));
        calcRequest.setEndDate(DateTimeUtil.parseDate(end));
        calcRequest.setProductCode(req.getProductId());
        calcRequest.setQuantity(req.getQunatity());
        //2021-06-02
        calcRequest.setChannelCode(req.getChannelCode());
        calcRequest.setFrom(req.getFrom());
        calcRequest.setPackageCode(req.getPackageId());
        calcRequest.setCategory(req.getCategory());
        PriceCalcResult priceCalcResult = null;
        calcRequest.setTraceId(traceId);
        calcRequest.setSource(req.getSource());
        try{
            BaseResponse<PriceCalcResult> priceCalcResultBaseResponse;
            if(StringUtils.isBlank(req.getCategory())){
                priceCalcResultBaseResponse = productService.calcTotalPrice(calcRequest);
            } else {
                priceCalcResultBaseResponse = productService.calcTotalPriceV2(calcRequest);
            }

            priceCalcResult = priceCalcResultBaseResponse.getData();
            //没有价格直接抛异常
            if(priceCalcResultBaseResponse.getCode()!=0||priceCalcResult==null){
                return BaseResponse.fail(CentralError.PRICE_CALC_PRICE_NOT_FOUND_ERROR);
            }
        }catch (HlCentralException e){
            log.error("大兄弟额  你的价格计算服务 又挂了=.=  :{}",e);
            return BaseResponse.fail(CentralError.PRICE_CALC_PRICE_NOT_FOUND_ERROR);
        }
        //总结算价不能小于0
        int amountFlag = priceCalcResult.getSettlesTotal().compareTo(BigDecimal.ZERO);
        if(amountFlag ==-1||amountFlag == 0){
            return BaseResponse.fail(CentralError.ERROR_AMOUNT_ORDER);
        }
        //总的结算价
        ycfCreateOrderReq.setAmount(priceCalcResult.getSettlesTotal());
        ycfCreateOrderReq.setFoodDetail(ycfBookFoods);
        ycfCreateOrderReq.setPriceDetail(ycfPriceItemList);
        ycfCreateOrderReq.setRoomDetail(ycfBookRooms);
        ycfCreateOrderReq.setTicketDetail(ycfBookTickets);
        //*********************************************组装供应商请求结束**********************************************
        //供应商对象包装业务实体类
        YcfBaseResult<YcfCreateOrderRes> ycfOrder =null;
        YcfCreateOrderRes ycfCreateOrderRes = null;
        ycfCreateOrderReq.setTraceId(traceId);
        try {
            ycfOrder = ycfOrderService.createOrder(ycfCreateOrderReq);
            if(ycfOrder!=null&&ycfOrder.getStatusCode()==200){
                ycfCreateOrderRes = ycfOrder.getData();
                if(ycfCreateOrderRes == null){
                    log.error("创建订单  供应商返回空对象 产品id:{}  供应商异常描述 ：{} , 【请求供应商json】 :{}",req.getProductId(),ycfOrder.getMessage(),JSONObject.toJSONString(ycfCreateOrderReq));
                    return SupplierErrorMsgTransfer.buildMsg(ycfOrder.getMessage());//异常消息以供应商返回的
                }
            }else{
                return BaseResponse.fail(CentralError.ERROR_ORDER);//异常消息以供应商返回的
            }
        }catch (HlCentralException e){
            log.error("ycfOrderService --> getCenterCreateOrder rpc服务异常 :{}",e);
            return BaseResponse.fail(CentralError.ERROR_ORDER);//异常消息以供应商返回的
        }
        createOrderRes = createOrderConverter.convertSupplierResponseToResponse(ycfCreateOrderRes);
        return BaseResponse.success(createOrderRes);
    }

    public BaseResponse<CenterPayOrderRes> getCenterPayOrder(PayOrderReq req){
        //封装中台创建订单返回结果
        CenterPayOrderRes payOrderRes = null;
        //转换前端传参
        YcfPayOrderReq ycfPayOrderReq = payOrderConverter.convertRequestToSupplierRequest(req);
        //供应商输出
        YcfBaseResult<YcfPayOrderRes> ycfPayOrder = null;
        YcfPayOrderRes ycfPayOrderRes = null;
        try {
            String traceId = req.getTraceId();
            if(StringUtils.isEmpty(traceId)){
                traceId = TraceIdUtils.getTraceId();
            }
            ycfPayOrderReq.setTraceId(traceId);
            ycfPayOrder = ycfOrderService.payOrder(ycfPayOrderReq);
            if(ycfPayOrder!=null&&ycfPayOrder.getStatusCode()==200){
                ycfPayOrderRes = ycfPayOrder.getData();
                if(ycfPayOrderRes == null){
                    log.error("支付订单  供应商返回空对象 本地订单号:{} ， 供应商异常描述 ：{} ,【请求供应商json】 :{}",req.getPartnerOrderId(),ycfPayOrder.getMessage(),JSONObject.toJSONString(ycfPayOrderReq));
                    switch (ycfPayOrder.getMessage()){
                        case "支付失败，对应支付流水号已存在" :
                            //todo 如果支付流水号是已存在的  通过查询订单详情校验一下再返回该异常
                            break;
                    }
                    return SupplierErrorMsgTransfer.buildMsg(ycfPayOrder.getMessage());//异常消息以供应商返回的
                }
            }else{
                return BaseResponse.fail(CentralError.ERROR_ORDER_PAY);
            }
        }catch (HlCentralException e){
            log.error("ycfOrderService --> getCenterPayOrder rpc服务异常 :{}",e);
            return BaseResponse.fail(CentralError.ERROR_ORDER_PAY);
        }
//        //测试数据
//        ycfPayOrder = new YcfBaseResult<>();
//        ycfPayOrderRes = new YcfPayOrderRes();
//        ycfPayOrderRes.setOrderId("ceshi123");
//        ycfPayOrderRes.setOrderStatus(1);
        ycfPayOrder.setData(ycfPayOrderRes);

        //封装中台返回结果
        payOrderRes = payOrderConverter.convertSupplierResponseToResponse(ycfPayOrderRes);
        //组装本地订单号参数
        if(payOrderRes != null){
            payOrderRes.setLocalOrderId(req.getPartnerOrderId());
        }
        return BaseResponse.success(payOrderRes);
    }

    public BaseResponse<CenterCancelOrderRes> getCenterCancelOrder(CancelOrderReq req) throws RuntimeException{
        //封装中台返回结果
        CenterCancelOrderRes cancelOrderRes = null;
        //转换前端传参
        YcfCancelOrderReq ycfCancelOrderReq = cancelOrderConverter.convertRequestToSupplierRequest(req);
        //供应商输出
        YcfBaseResult<YcfCancelOrderRes> ycfBaseResult=null;
        YcfCancelOrderRes ycfCancelOrderRes = null;
        try {
            String traceId = req.getTraceId();
            if(StringUtils.isEmpty(traceId)){
                traceId = TraceIdUtils.getTraceId();
            }
            ycfCancelOrderReq.setTraceId(traceId);
            ycfBaseResult = ycfOrderService.cancelOrder(ycfCancelOrderReq);
            if(ycfBaseResult!=null&&ycfBaseResult.getStatusCode()==200){
                ycfCancelOrderRes = ycfBaseResult.getData();
                if(ycfCancelOrderRes == null){
                    log.error("取消订单  供应商返回空对象 传的订单号：{} 产品编号：{} 供应商异常描述 ：{}  ,【请求供应商json】 :{}",req.getPartnerOrderId(),req.getProductCode(),ycfBaseResult.getMessage(),JSONObject.toJSONString(ycfCancelOrderReq));
                    return SupplierErrorMsgTransfer.buildMsg(ycfBaseResult.getMessage());//异常消息以供应商返回的
                }
            }else{
                return BaseResponse.fail(CentralError.ERROR_SUPPLIER_CANCEL_ORDER);
            }
        }catch (HlCentralException e){
            log.error("ycfOrderService --> getCenterPayOrder rpc服务异常 ：{}",e);
            return BaseResponse.fail(CentralError.ERROR_SUPPLIER_CANCEL_ORDER);
        }
        //组装中台返回结果
        cancelOrderRes = cancelOrderConverter.convertSupplierResponseToResponse(ycfCancelOrderRes);
        return BaseResponse.success(cancelOrderRes);
    }

    public  BaseResponse<CenterPayCheckRes> payCheck(PayOrderReq req){
        //支付前校验逻辑 判断订单状态是否是待支付
        String traceId = req.getTraceId();
        if(StringUtils.isEmpty(traceId)){
            traceId = TraceIdUtils.getTraceId();
        }
        CenterPayCheckRes result = new CenterPayCheckRes();
        OrderOperReq operReq = new OrderOperReq();
        operReq.setOrderId(req.getPartnerOrderId());
        operReq.setChannelCode(req.getChannelCode());
        operReq.setTraceId(req.getTraceId());
        operReq.setTraceId(traceId);
        try {
            BaseResponse<OrderDetailRep> orderDetail = this.getOrderDetail(operReq);
            if(orderDetail.getCode()==0&&orderDetail.getData()!=null&&orderDetail.getData().getOrderStatus()== OrderStatus.TO_BE_PAID.getCode()){
                result.setResult(true);
                return BaseResponse.success(result);
            }else {
                log.error("支付前校验没有通过 订单详情请求json:{}",JSONObject.toJSONString(operReq));
                log.error("支付前校验没有通过 订单详情返回json:{}",JSONObject.toJSONString(orderDetail));
                return BaseResponse.fail(CentralError.ERROR_ORDER_PAY_BEFORE);
            }
        }catch (HlCentralException e){
            log.error("支付前校验失败 ：{}",e);
            return BaseResponse.fail(CentralError.ERROR_ORDER_PAY_BEFORE);
        }
    }

    public BaseResponse<CenterCancelOrderRes> getCenterApplyRefund(CancelOrderReq req){
        //封装中台返回结果
        CenterCancelOrderRes applyRefundRes = null;
        //转换前端传参
        YcfCancelOrderReq ycfCancelOrderReq = applyRefundConverter.convertRequestToSupplierRequest(req);
        //供应商输出
        YcfBaseResult<YcfCancelOrderRes> ycfBaseResult = null;
        YcfCancelOrderRes ycfCancelOrderRes = null;
        try {
            String traceId = req.getTraceId();
            if(StringUtils.isEmpty(traceId)){
                traceId = TraceIdUtils.getTraceId();
            }
            ycfCancelOrderReq.setTraceId(traceId);
            ycfBaseResult = ycfOrderService.cancelOrder(ycfCancelOrderReq);
            if(ycfBaseResult!=null&&ycfBaseResult.getStatusCode()==200){
                ycfCancelOrderRes = ycfBaseResult.getData();
                if(ycfCancelOrderRes == null){
                    log.error("申请退款  供应商返回空对象 产品编号：{} 供应商异常描述 ：{} ,【请求供应商json】 :{}",req.getPartnerOrderId(),req.getProductCode(),ycfBaseResult.getMessage(),JSONObject.toJSONString(ycfCancelOrderReq));
                    return SupplierErrorMsgTransfer.buildMsg(ycfBaseResult.getMessage());//异常消YcfOrderManger息以供应商返回的
                }
            }else{
                return BaseResponse.fail(CentralError.ERROR_SUPPLIER_APPLYREFUND_ORDER);
            }
        }catch (HlCentralException e){
            log.error("ycfOrderService --> getCenterPayOrder rpc服务异常 ：{}",e);
            return BaseResponse.fail(CentralError.ERROR_SUPPLIER_APPLYREFUND_ORDER);
        }
        //组装中台返回结果
        applyRefundRes = applyRefundConverter.convertSupplierResponseToResponse(ycfCancelOrderRes);
        return BaseResponse.success(applyRefundRes);
    }

    public void refreshStockPrice(ProductPriceReq req){

        try {
            String traceId = req.getTraceId();
            if(StringUtils.isEmpty(traceId)){
                traceId = TraceIdUtils.getTraceId();
            }
            YcfGetPriceRequest stockPriceReq=new YcfGetPriceRequest();
            stockPriceReq.setProductID(req.getSupplierProductId());
            stockPriceReq.setPartnerProductID(req.getProductCode());
            stockPriceReq.setStartDate(req.getStartDate());
            stockPriceReq.setEndDate(req.getEndDate());
            stockPriceReq.setTraceId(traceId);
            ycfSynService.getPrice(stockPriceReq);

        } catch (Exception e) {
            log.info("",e);
        }
    }

    @Override
    public void syncPrice(String productCode, String supplierProductId, String startDate, String endDate, String traceId){
        try {
            YcfGetPriceRequest request = new YcfGetPriceRequest();
            if(StringUtils.isEmpty(traceId)){
                traceId = TraceIdUtils.getTraceId();
            }
            request.setProductID(supplierProductId);
            request.setPartnerProductID(productCode);
            request.setStartDate(startDate);
            request.setEndDate(endDate);
            request.setTraceId(traceId);
            ycfSynService.getPrice(request);
        } catch (Exception e) {
            log.error("刷新价格异常，", e);
        }
    }

    @Override
    public void syncPriceV2(String productCode, String supplierProductId, String startDate, String endDate, String traceId){
        try {
            YcfGetPriceRequest request = new YcfGetPriceRequest();
            if(StringUtils.isEmpty(traceId)){
                traceId = TraceIdUtils.getTraceId();
            }
            request.setProductID(supplierProductId);
            request.setPartnerProductID(productCode);
            request.setStartDate(startDate);
            request.setEndDate(endDate);
            request.setTraceId(traceId);
            ycfSynService.syncPriceV2(request);
        } catch (Exception e) {
            log.error("刷新价格异常，", e);
        }
    }

    /**
     * 判断时间跨度超过90天
     * @param begin
     * @param end
     * @return
     */
    public Boolean isOutTime(Date begin,Date end){
        long margin = end.getTime() - begin.getTime();
        margin = margin / (1000 * 60 * 60 * 24);
        return margin > 90;
    }
}
