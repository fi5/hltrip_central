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
 * ?????????desc<br>
 * ?????????Copyright (c) 2011-2020<br>
 * ?????????????????????<br>
 * ??????????????????<br>
 * ?????????1.0<br>
 * ???????????????2020/7/1<br>
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
        //??????????????????????????????
        if(StringUtils.isBlank(end)){
            end = begin;
        }
        //?????????????????????????????????
        YcfBookCheckReq ycfBookCheckReq = new YcfBookCheckReq();
        //????????????productId
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
         * ??????????????????????????????
         */
        /*if(DateTimeUtil.parseDate(begin).after(DateTimeUtil.parseDate(begin))){
            log.error("??????????????? ?????????????????????????????? ?????? ???????????????{}",req.getProductId());
            return BaseResponse.fail(CentralError.ERROR_DATE_ORDER_1);
        }*/
        /**
         * ??????????????????90???
         */
      /*  if(this.isOutTime(DateTimeUtil.parseDate(begin),DateTimeUtil.parseDate(begin))){
            log.error("??????????????? ??????????????????90??? ?????? ???????????????{}",req.getProductId());
            return BaseResponse.fail(CentralError.ERROR_DATE_ORDER_2);
        }*/
        //????????????????????????
        YcfBookCheckRes ycfBookCheckRes = null;
        String traceId = req.getTraceId();
        if(StringUtils.isEmpty(traceId)){
            traceId = TraceIdUtils.getTraceId();
        }
        ycfBookCheckReq.setTraceId(traceId);
        try {
            //???????????????
            YcfBaseResult<YcfBookCheckRes> checkInfos = ycfOrderService.getCheckInfos(ycfBookCheckReq);
            if(checkInfos!=null&&checkInfos.getStatusCode()==200){
                ycfBookCheckRes = checkInfos.getData();
                if(ycfBookCheckRes == null){
                    log.error("???????????????  ???????????????????????? ??????id:{}  ????????????????????? ???{},??????????????????json??? :{}",req.getProductId(),checkInfos.getMessage(), JSONObject.toJSONString(ycfBookCheckReq));
                    return SupplierErrorMsgTransfer.buildMsg(checkInfos.getMessage());//?????????????????????????????????
                }
            }else{
                return BaseResponse.fail(CentralError.ERROR_SUPPLIER_BOOK_CHECK_ORDER);
            }
        }catch (HlCentralException e){
            log.error("ycfOrderService --> getNBCheckInfos rpc???????????? ???{}",e);
            return BaseResponse.fail(CentralError.ERROR_SUPPLIER_BOOK_CHECK_ORDER);
        }
        //????????????
        CenterBookCheck  bookCheck = new CenterBookCheck();
        //???????????????????????????
        List<YcfBookSaleInfo> saleInfos = ycfBookCheckRes.getSaleInfos();
        //?????????????????????
        List<Integer> stockList = new ArrayList<>();
        //????????????
        if(CollectionUtils.isEmpty(saleInfos)) {
            log.error("???????????????  ?????????????????????saleInfos ??????id:{}  ",req.getProductId());
            return BaseResponse.fail(CentralError.NO_STOCK_ERROR);
        }
        saleInfos.forEach(ycfBookSaleInfo -> {
            stockList.add(ycfBookSaleInfo.getTotalStock());
        });
        //???????????????????????????????????????????????????????????????????????????????????????????????????????????????
        if(stockList.size()>0){
            if(stockList.size()>1){
                Collections.sort(stockList);
            }
            //?????????????????????????????????????????????????????????
            if(req.getCount()>stockList.get(0)){
                bookCheck.setStock(stockList.get(0));
                log.info("???????????????????????????????????? ???????????????{}",req.getProductId());
                return BaseResponse.withFail(CentralError.NOTENOUGH_STOCK_ERROR,bookCheck);
            }
        }
        //?????????????????????????????????
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
            //???????????????????????????
            if(priceCalcResultBaseResponse.getCode()!=0||priceCalcResult==null){
                return BaseResponse.fail(CentralError.PRICE_CALC_PRICE_NOT_FOUND_ERROR);
            }
        }catch (HlCentralException e){
            log.error("????????????  ???????????????????????? ?????????=.=  :{}",e);
            return BaseResponse.fail(CentralError.PRICE_CALC_PRICE_NOT_FOUND_ERROR);
        }
        //??????????????????
        bookCheck.setSettlePrice(priceCalcResult.getSettlesTotal());
        //????????????
        bookCheck.setSalePrice(priceCalcResult.getSalesTotal());
        //????????????
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
            log.info("?????????????????????:"+JSONObject.toJSONString(order));
            final YcfOrderStatusResult data = order.getData();
            //??????????????????,???????????????
            if(order.getStatusCode()!=200 || !order.getSuccess())
                return BaseResponse.fail(CentralError.ERROR_NO_ORDER);//?????????????????????????????????
            OrderDetailRep rep=new OrderDetailRep();
            rep.setOrderId(data.getOrderId());
            //?????????consumer?????????????????????
            rep.setOrderStatus(OrderInfoTranser.genCommonOrderStatus(data.getOrderStatus(),1));
            rep.setVochers(JSONObject.parseArray(JSONObject.toJSONString(data.getVochers()), OrderDetailRep.Voucher.class));
            return BaseResponse.success(rep);
        } catch (Exception e) {
            log.error("YCFgetOrderDetail??????:"+JSONObject.toJSONString(req),e);
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
            log.info("???????????????:"+JSONObject.toJSONString(vochers));
            final YcfVouchersResult data = vochers.getData();
            if(vochers.getStatusCode()!=200 || !vochers.getSuccess())
                return BaseResponse.fail(OrderInfoTranser.findCentralError(vochers.getMessage()));//?????????????????????????????????
            OrderDetailRep rep=new OrderDetailRep();
            rep.setOrderId(req.getOrderId());
            rep.setVochers(JSONObject.parseArray(JSONObject.toJSONString(data.getVochers()), OrderDetailRep.Voucher.class));
            return BaseResponse.success(rep);
        } catch (Exception e) {
            log.error("YCFgetVochers??????:"+JSONObject.toJSONString(req),e);
            return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
        }

    }

    public BaseResponse<CenterCreateOrderRes> getCenterCreateOrder(CreateOrderReq req){
        //??????????????????
        CenterCreateOrderRes createOrderRes = null;
        String begin = req.getBeginDate();
        String end = req.getEndDate();
        //??????????????????????????????
        if(StringUtils.isBlank(end)){
            end = begin;
        }
        //???????????????????????????
        BookCheckReq bookCheckReq = new BookCheckReq();
        //????????????productId
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
        //?????????????????????
        BaseResponse<CenterBookCheck> centerCheckInfos = this.getCenterCheckInfos(bookCheckReq);
        if(centerCheckInfos.getCode() != 0){
            log.error("????????????????????????????????????????????????????????????{}?????????????????????",req.getProductId());
            return BaseResponse.fail(CentralError.ERROR_BOOKBEFORE_ORDER);
        }
        //??????????????????????????????0
        if(req.getQunatity()<=0){
            return BaseResponse.fail(CentralError.PRICE_CALC_QUANTITY_LIMIT_ERROR);
        }
        //*********************************************???????????????????????????**********************************************
        //**???????????????**
        List<YcfBookFood> ycfBookFoods = new ArrayList<>();
        //**?????????????????????**
        List<YcfBookRoom> ycfBookRooms = new ArrayList<>();
        //**????????????????????????**
        List<YcfBookTicket> ycfBookTickets = new ArrayList<>();
        //?????????????????????
        List<YcfPriceItem> ycfPriceItemList = new ArrayList<>();
        //??????????????????????????????
        YcfCreateOrderReq ycfCreateOrderReq = createOrderConverter.convertRequestToSupplierRequest(req);
            if(StringUtils.isBlank(req.getCategory())){
            //????????????????????????
            ProductPO productPO = productDao.getTripProductByCode(req.getProductId());
            //????????????
            FoodPO food = productPO.getFood();
            List<FoodInfoPO> foods = food.getFoods();
            //???????????????
            RoomPO room = productPO.getRoom();
            List<RoomInfoPO> rooms = room.getRooms();
            //???????????????
            TicketPO ticket = productPO.getTicket();
            List<TicketInfoPO> tickets = ticket.getTickets();

                //**???????????????**
                if(!CollectionUtils.isEmpty(foods)){
                    foods.forEach(f ->{
                        YcfBookFood ycfBookFood = new YcfBookFood();
                        ycfBookFood.setFoodId(f.getSupplierResourceId());
                        ycfBookFood.setCheckInDate(req.getBeginDate());
                        ycfBookFoods.add(ycfBookFood);
                    });
                }
                //**?????????????????????**
                if(!CollectionUtils.isEmpty(rooms)){
                    rooms.forEach(roomInfoPO ->{
                        YcfBookRoom ycfBookRoom = new YcfBookRoom();
                        ycfBookRoom.setCheckInDate(req.getBeginDate());
                        ycfBookRoom.setCheckOutDate(req.getEndDate());
                        ycfBookRoom.setRoomId(roomInfoPO.getSupplierResourceId());
                        ycfBookRooms.add(ycfBookRoom);
                    });
                }
                //**????????????????????????**
                if(!CollectionUtils.isEmpty(tickets)){
                    tickets.forEach(ticketInfoPO ->{
                        YcfBookTicket ycfBookTicket = new YcfBookTicket();
                        ycfBookTicket.setCheckInDate(req.getBeginDate());
                        ycfBookTicket.setTicketId(ticketInfoPO.getSupplierResourceId());
                        ycfBookTickets.add(ycfBookTicket);
                    });
                }

            //????????????????????????
            PricePO pricePos = productDao.getPricePos(req.getProductId());
            if(pricePos!=null&&!CollectionUtils.isEmpty(pricePos.getPriceInfos())){
                //?????????????????????????????????,???????????????????????????????????????????????????  ??????????????????????????????????????????
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
                        //????????????
                        YcfPriceItem ycfPriceItem = new YcfPriceItem();
                        //YYYYMMDD
                        ycfPriceItem.setDate(DateTimeUtil.trancateToDate(price.getSaleDate()));
                        ycfPriceItem.setPrice(price.getSettlePrice());
                        ycfPriceItemList.add(ycfPriceItem);
                    };
                }
            }else{
                log.error("???????????? ?????????????????????????????????????????? ??????code: {}",req.getProductId());
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
                        //????????????
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
                        //????????????
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
            //????????????????????????????????????(???????????????)
            if(CollectionUtils.isEmpty(foods)
                    && CollectionUtils.isEmpty(rooms)
                    && CollectionUtils.isEmpty(tickets)){
                log.error("??????????????????????????????,????????????????????????????????????(???????????????) ???????????????{}",req.getProductId());
                return BaseResponse.fail(CentralError.ERROR_ORDER);
            }
            //**???????????????**
            if(!CollectionUtils.isEmpty(foods)){
                foods.forEach(f ->{
                    YcfBookFood ycfBookFood = new YcfBookFood();
                    ycfBookFood.setFoodId(f.getFoodId());
                    ycfBookFood.setCheckInDate(req.getBeginDate());
                    ycfBookFoods.add(ycfBookFood);
                });
            }
            //**?????????????????????**
            if(!CollectionUtils.isEmpty(rooms)){
                rooms.forEach(roomInfoPO ->{
                    YcfBookRoom ycfBookRoom = new YcfBookRoom();
                    ycfBookRoom.setCheckInDate(req.getBeginDate());
                    ycfBookRoom.setCheckOutDate(req.getEndDate());
                    ycfBookRoom.setRoomId(roomInfoPO.getRoomId());
                    ycfBookRooms.add(ycfBookRoom);
                });
            }
            //**????????????????????????**
            if(!CollectionUtils.isEmpty(tickets)){
                tickets.forEach(ticketInfoPO ->{
                    YcfBookTicket ycfBookTicket = new YcfBookTicket();
                    ycfBookTicket.setCheckInDate(req.getBeginDate());
                    ycfBookTicket.setTicketId(ticketInfoPO.getTicketId());
                    ycfBookTickets.add(ycfBookTicket);
                });
            }
        }

        //?????????????????????????????????
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
            //???????????????????????????
            if(priceCalcResultBaseResponse.getCode()!=0||priceCalcResult==null){
                return BaseResponse.fail(CentralError.PRICE_CALC_PRICE_NOT_FOUND_ERROR);
            }
        }catch (HlCentralException e){
            log.error("????????????  ???????????????????????? ?????????=.=  :{}",e);
            return BaseResponse.fail(CentralError.PRICE_CALC_PRICE_NOT_FOUND_ERROR);
        }
        //????????????????????????0
        int amountFlag = priceCalcResult.getSettlesTotal().compareTo(BigDecimal.ZERO);
        if(amountFlag ==-1||amountFlag == 0){
            return BaseResponse.fail(CentralError.ERROR_AMOUNT_ORDER);
        }
        //???????????????
        ycfCreateOrderReq.setAmount(priceCalcResult.getSettlesTotal());
        ycfCreateOrderReq.setFoodDetail(ycfBookFoods);
        ycfCreateOrderReq.setPriceDetail(ycfPriceItemList);
        ycfCreateOrderReq.setRoomDetail(ycfBookRooms);
        ycfCreateOrderReq.setTicketDetail(ycfBookTickets);
        //*********************************************???????????????????????????**********************************************
        //????????????????????????????????????
        YcfBaseResult<YcfCreateOrderRes> ycfOrder =null;
        YcfCreateOrderRes ycfCreateOrderRes = null;
        ycfCreateOrderReq.setTraceId(traceId);
        try {
            ycfOrder = ycfOrderService.createOrder(ycfCreateOrderReq);
            if(ycfOrder!=null&&ycfOrder.getStatusCode()==200){
                ycfCreateOrderRes = ycfOrder.getData();
                if(ycfCreateOrderRes == null){
                    log.error("????????????  ???????????????????????? ??????id:{}  ????????????????????? ???{} , ??????????????????json??? :{}",req.getProductId(),ycfOrder.getMessage(),JSONObject.toJSONString(ycfCreateOrderReq));
                    return SupplierErrorMsgTransfer.buildMsg(ycfOrder.getMessage());//?????????????????????????????????
                }
            }else{
                return BaseResponse.fail(CentralError.ERROR_ORDER);//?????????????????????????????????
            }
        }catch (HlCentralException e){
            log.error("ycfOrderService --> getCenterCreateOrder rpc???????????? :{}",e);
            return BaseResponse.fail(CentralError.ERROR_ORDER);//?????????????????????????????????
        }
        createOrderRes = createOrderConverter.convertSupplierResponseToResponse(ycfCreateOrderRes);
        return BaseResponse.success(createOrderRes);
    }

    public BaseResponse<CenterPayOrderRes> getCenterPayOrder(PayOrderReq req){
        //????????????????????????????????????
        CenterPayOrderRes payOrderRes = null;
        //??????????????????
        YcfPayOrderReq ycfPayOrderReq = payOrderConverter.convertRequestToSupplierRequest(req);
        //???????????????
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
                    log.error("????????????  ???????????????????????? ???????????????:{} ??? ????????????????????? ???{} ,??????????????????json??? :{}",req.getPartnerOrderId(),ycfPayOrder.getMessage(),JSONObject.toJSONString(ycfPayOrderReq));
                    switch (ycfPayOrder.getMessage()){
                        case "?????????????????????????????????????????????" :
                            //todo ????????????????????????????????????  ??????????????????????????????????????????????????????
                            break;
                    }
                    return SupplierErrorMsgTransfer.buildMsg(ycfPayOrder.getMessage());//?????????????????????????????????
                }
            }else{
                return BaseResponse.fail(CentralError.ERROR_ORDER_PAY);
            }
        }catch (HlCentralException e){
            log.error("ycfOrderService --> getCenterPayOrder rpc???????????? :{}",e);
            return BaseResponse.fail(CentralError.ERROR_ORDER_PAY);
        }
//        //????????????
//        ycfPayOrder = new YcfBaseResult<>();
//        ycfPayOrderRes = new YcfPayOrderRes();
//        ycfPayOrderRes.setOrderId("ceshi123");
//        ycfPayOrderRes.setOrderStatus(1);
        ycfPayOrder.setData(ycfPayOrderRes);

        //????????????????????????
        payOrderRes = payOrderConverter.convertSupplierResponseToResponse(ycfPayOrderRes);
        //???????????????????????????
        if(payOrderRes != null){
            payOrderRes.setLocalOrderId(req.getPartnerOrderId());
        }
        return BaseResponse.success(payOrderRes);
    }

    public BaseResponse<CenterCancelOrderRes> getCenterCancelOrder(CancelOrderReq req) throws RuntimeException{
        //????????????????????????
        CenterCancelOrderRes cancelOrderRes = null;
        //??????????????????
        YcfCancelOrderReq ycfCancelOrderReq = cancelOrderConverter.convertRequestToSupplierRequest(req);
        //???????????????
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
                    log.error("????????????  ???????????????????????? ??????????????????{} ???????????????{} ????????????????????? ???{}  ,??????????????????json??? :{}",req.getPartnerOrderId(),req.getProductCode(),ycfBaseResult.getMessage(),JSONObject.toJSONString(ycfCancelOrderReq));
                    return SupplierErrorMsgTransfer.buildMsg(ycfBaseResult.getMessage());//?????????????????????????????????
                }
            }else{
                return BaseResponse.fail(CentralError.ERROR_SUPPLIER_CANCEL_ORDER);
            }
        }catch (HlCentralException e){
            log.error("ycfOrderService --> getCenterPayOrder rpc???????????? ???{}",e);
            return BaseResponse.fail(CentralError.ERROR_SUPPLIER_CANCEL_ORDER);
        }
        //????????????????????????
        cancelOrderRes = cancelOrderConverter.convertSupplierResponseToResponse(ycfCancelOrderRes);
        return BaseResponse.success(cancelOrderRes);
    }

    public  BaseResponse<CenterPayCheckRes> payCheck(PayOrderReq req){
        //????????????????????? ????????????????????????????????????
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
                log.error("??????????????????????????? ??????????????????json:{}",JSONObject.toJSONString(operReq));
                log.error("??????????????????????????? ??????????????????json:{}",JSONObject.toJSONString(orderDetail));
                return BaseResponse.fail(CentralError.ERROR_ORDER_PAY_BEFORE);
            }
        }catch (HlCentralException e){
            log.error("????????????????????? ???{}",e);
            return BaseResponse.fail(CentralError.ERROR_ORDER_PAY_BEFORE);
        }
    }

    public BaseResponse<CenterCancelOrderRes> getCenterApplyRefund(CancelOrderReq req){
        //????????????????????????
        CenterCancelOrderRes applyRefundRes = null;
        //??????????????????
        YcfCancelOrderReq ycfCancelOrderReq = applyRefundConverter.convertRequestToSupplierRequest(req);
        //???????????????
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
                    log.error("????????????  ???????????????????????? ???????????????{} ????????????????????? ???{} ,??????????????????json??? :{}",req.getPartnerOrderId(),req.getProductCode(),ycfBaseResult.getMessage(),JSONObject.toJSONString(ycfCancelOrderReq));
                    return SupplierErrorMsgTransfer.buildMsg(ycfBaseResult.getMessage());//?????????YcfOrderManger????????????????????????
                }
            }else{
                return BaseResponse.fail(CentralError.ERROR_SUPPLIER_APPLYREFUND_ORDER);
            }
        }catch (HlCentralException e){
            log.error("ycfOrderService --> getCenterPayOrder rpc???????????? ???{}",e);
            return BaseResponse.fail(CentralError.ERROR_SUPPLIER_APPLYREFUND_ORDER);
        }
        //????????????????????????
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
            log.error("?????????????????????", e);
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
            log.error("?????????????????????", e);
        }
    }

    /**
     * ????????????????????????90???
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
