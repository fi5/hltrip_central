package com.huoli.trip.central.web.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.huoli.trip.central.api.ProductService;
import com.huoli.trip.central.api.ProductV2Service;
import com.huoli.trip.central.web.constant.ColourConstants;
import com.huoli.trip.central.web.dao.*;
import com.huoli.trip.central.web.service.CommonService;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.entity.mpo.ProductListMPO;
import com.huoli.trip.common.entity.mpo.ScenicProductSortMPO;
import com.huoli.trip.common.entity.mpo.groupTour.GroupTourPrice;
import com.huoli.trip.common.entity.mpo.groupTour.GroupTourProductMPO;
import com.huoli.trip.common.entity.mpo.groupTour.GroupTourProductSetMealMPO;
import com.huoli.trip.common.entity.mpo.hotel.HotelMPO;
import com.huoli.trip.common.entity.mpo.hotelScenicSpot.*;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.*;
import com.huoli.trip.common.util.DateTimeUtil;
import com.huoli.trip.common.util.ListUtils;
import com.huoli.trip.common.vo.IncreasePrice;
import com.huoli.trip.common.vo.IncreasePriceCalendar;
import com.huoli.trip.common.vo.PriceInfo;
import com.huoli.trip.common.vo.request.goods.GroupTourListReq;
import com.huoli.trip.common.vo.request.v2.*;
import com.huoli.trip.common.vo.response.BaseResponse;
import com.huoli.trip.common.vo.response.central.ProductPriceCalendarResult;
import com.huoli.trip.common.vo.v2.*;
import com.huoli.trip.data.api.DataService;
import com.huoli.trip.data.api.ProductDataService;
import com.huoli.trip.data.vo.ChannelInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lunatic
 * @Title:
 * @Package
 * @Description:
 * @date 2021/4/2715:19
 */
@Slf4j
@Service(timeout = 10000, group = "hltrip")
public class ProductV2ServiceImpl implements ProductV2Service {
    @Autowired
    ScenicSpotDao scenicSpotDao;

    @Autowired
    GroupTourDao groupTourDao;

    @Autowired
    HotelScenicDao hotelScenicDao;

    @Autowired
    private CommonService commonService;

    @Autowired
    private ProductDao productDao;

    @Autowired
    private ProductService productService;

    @Autowired
    private ScenicProductSortDao scenicProductSortDao;

    @Autowired
    private ScenicSpotProductPriceDao scenicSpotProductPriceDao;

    @Reference(group = "hltrip", timeout = 30000, check = false, retries = 3)
    DataService dataService;

    @Reference(group = "hltrip", timeout = 30000, check = false, retries = 3)
    ProductDataService productDataService;

    @Override
    public BaseResponse<ScenicSpotBase> querycScenicSpotBase(ScenicSpotRequest request) {
        ScenicSpotMPO scenicSpotMPO = scenicSpotDao.qyerySpotById(request.getScenicSpotId());
        ScenicSpotBase scenicSpotBase = null;
        if(scenicSpotMPO != null){
            scenicSpotBase = new ScenicSpotBase();
            BeanUtils.copyProperties(scenicSpotMPO,scenicSpotBase);
        }
        return BaseResponse.withSuccess(scenicSpotBase);
    }

    /**
     * @author: wangying
     * @date 5/17/21 4:39 PM
     * ?????????????????????
     * [request]
     * @return {@link BaseResponse< GroupTourBody>}
     * @throws
     */
    @Override
    public BaseResponse<GroupTourBody> queryGroupTourById(GroupTourRequest request) {
        List<String> channelInfo = new ArrayList<>();
        if (!StringUtils.equals(request.getFrom(), "order")) {
            BaseResponse<List<ChannelInfo>> listBaseResponse = dataService.queryChannelInfo(1);
            if(listBaseResponse.getCode() == 0 && listBaseResponse.getData() != null){
                channelInfo = listBaseResponse.getData().stream().map(a -> a.getChannel()).collect(Collectors.toList());
            }
        }
        GroupTourBody groupTourProductBody = null;
        final GroupTourProductMPO groupTourProductMPO = groupTourDao.queryTourProduct(request.getGroupTourId(), channelInfo);
        if(groupTourProductMPO == null){
            return BaseResponse.fail(CentralError.ERROR_NO_PRODUCT_WITHDRAW_SUPPLIER);
        }
        List<String> depCodes = Lists.newArrayList();
        String cityCode = request.getCityCode();
        if(CollectionUtils.isNotEmpty(groupTourProductMPO.getDepInfos()) && StringUtils.isNotBlank(cityCode)){
            groupTourProductMPO.getDepInfos().stream().forEach(item -> {
                if(StringUtils.equalsIgnoreCase(cityCode, item.getCityCode())
                    ||StringUtils.equalsIgnoreCase(cityCode, item.getProvinceCode())
                    ||StringUtils.equalsIgnoreCase(cityCode, item.getDestinationCode())){
                    depCodes.add(item.getDestinationCode());
                }
            });
        }
        final List<GroupTourProductSetMealMPO> groupTourProductSetMealMPOS = groupTourDao.queryProductSetMealByProductId(groupTourProductMPO.getId(), depCodes, request.getPackageId(), request.getDate());
        log.info("???????????????????????????{}", JSONObject.toJSONString(groupTourProductSetMealMPOS));
        if(ListUtils.isEmpty(groupTourProductSetMealMPOS)){
            log.error("??????????????? {} ?????????????????????????????????");
            productDataService.updateProduct(1, groupTourProductMPO.getId(), 1);
            return BaseResponse.fail(CentralError.ERROR_NO_PRODUCT_SET_MEAL);
        }
        groupTourProductBody = new GroupTourBody();
        BeanUtils.copyProperties(groupTourProductMPO,groupTourProductBody);
        List<GroupTourProductSetMeal> meals = groupTourProductSetMealMPOS.stream().map(p->{
            GroupTourProductSetMeal groupTourProductSetMeal = new GroupTourProductSetMeal();
            BeanUtils.copyProperties(p,groupTourProductSetMeal);
            //????????????
            IncreasePrice increasePrice = new IncreasePrice();
            increasePrice.setChannelCode(groupTourProductMPO.getChannel());
            increasePrice.setProductCode(groupTourProductMPO.getId());
            increasePrice.setAppSource(request.getFrom());
            increasePrice.setAppSubSource(request.getSource());
            increasePrice.setProductCategory(groupTourProductMPO.getCategory());
            List<GroupTourPrice> groupTourPrices = p.getGroupTourPrices();
            List<IncreasePriceCalendar> priceCalendars = groupTourPrices.stream().map(item -> {
                IncreasePriceCalendar priceCalendar = new IncreasePriceCalendar();
                priceCalendar.setAdtSellPrice(item.getAdtSellPrice());
                priceCalendar.setChdSellPrice(item.getChdSellPrice());
                priceCalendar.setDate(item.getDate());

                priceCalendar.setAdtSettlePrice(item.getAdtPrice());
                priceCalendar.setAdtFloatPriceType(item.getAdtFloatPriceType());
                priceCalendar.setAdtFloatPrice(item.getAdtFloatPrice());
                priceCalendar.setAdtFloatPriceManually(item.isAdtFloatPriceManually());

                priceCalendar.setChdFloatPrice(item.getChdFloatPrice());
                priceCalendar.setChdFloatPriceType(item.getChdFloatPriceType());
                priceCalendar.setChdSettlePrice(item.getChdPrice());
                priceCalendar.setChdFloatPriceManually(item.isChdFloatPriceManually());
                return priceCalendar;
            }).collect(Collectors.toList());
            increasePrice.setPrices(priceCalendars);
            commonService.increasePrice(increasePrice);

            Map<String, IncreasePriceCalendar> priceMap = Maps.uniqueIndex(increasePrice.getPrices(), a -> a.getDate());

            groupTourPrices.stream().forEach(item -> {
                IncreasePriceCalendar priceCalendar = priceMap.get(item.getDate());
                item.setAdtSellPrice(priceCalendar.getAdtSellPrice());
                item.setChdSellPrice(priceCalendar.getChdSellPrice());
            });
            p.setGroupTourPrices(groupTourPrices);
            return groupTourProductSetMeal;
        }).collect(Collectors.toList());
        groupTourProductBody.setSetMeals(meals);
        //??????????????????
        GroupTourListReq groupTourListReq = new GroupTourListReq();
        if(CollectionUtils.isNotEmpty(depCodes) && depCodes.size() > 1){
            groupTourListReq.setDepCityCode(request.getCityCode());
        }else{
            groupTourListReq.setDepPlace(request.getCityCode());
        }
        groupTourListReq.setArrCityCode(request.getArrCity());
        groupTourListReq.setGroupTourType(groupTourProductMPO.getGroupTourType());
        groupTourListReq.setApp(request.getFrom());
        List<ProductListMPO> productListMPOS = productDao.groupTourList(groupTourListReq, channelInfo);
        log.info("???????????????{}", JSONObject.toJSONString(productListMPOS));
        if (CollectionUtils.isNotEmpty(productListMPOS)) {
            List<GroupTourRecommend> groupTourRecommends = productListMPOS.stream().map(a -> {
                GroupTourRecommend groupTourRecommend = new GroupTourRecommend();
                groupTourRecommend.setCategory(a.getCategory());
                groupTourRecommend.setImage(a.getProductImageUrl());
                groupTourRecommend.setProductId(a.getProductId());
                groupTourRecommend.setProductName(a.getProductName());
                IncreasePrice increasePrice = productService.increasePrice(a, request.getFrom(), request.getSource());
                groupTourRecommend.setPrice(increasePrice.getPrices().get(0).getAdtSellPrice());
                return groupTourRecommend;
            }).collect(Collectors.toList());
            groupTourProductBody.setRecommends(groupTourRecommends);
        }
        return BaseResponse.withSuccess(groupTourProductBody);
    }

    @Override
    public GroupMealsBody groupMealsBody(GroupTourMealsRequest request) {
        GroupMealsBody body = null;
        List<GroupTourProductSetMealMPO> groupTourProductSetMealMPOS = groupTourDao.queryProductSetMealByProductId(request.getGroupTourId(), null, request.getPackageId(), request.getDate());
        if (CollectionUtils.isNotEmpty(groupTourProductSetMealMPOS)) {
            List<GroupTourProductSetMeal> meals = groupTourProductSetMealMPOS.stream().map(p -> {
                GroupTourProductSetMeal groupTourProductSetMeal = new GroupTourProductSetMeal();
                BeanUtils.copyProperties(p, groupTourProductSetMeal);
                return groupTourProductSetMeal;
            }).collect(Collectors.toList());
            body.setSetMeals(meals);
        }
        return body;
    }

    @Override
    public BaseResponse<List<ScenicSpotProductBase>> queryScenicSpotProduct(ScenicSpotProductRequest request) {

        BaseResponse<List<ChannelInfo>> listBaseResponse = dataService.queryChannelInfo(1);
        List<String> channelInfo = new ArrayList<>();
        if(listBaseResponse.getCode() == 0 && listBaseResponse.getData() != null){
            channelInfo = listBaseResponse.getData().stream().map(a -> a.getChannel()).collect(Collectors.toList());
        }
        List<ScenicSpotProductMPO> scenicSpotProductMPOS = scenicSpotDao.querySpotProduct(request.getScenicSpotId(), channelInfo);
        ScenicSpotMPO scenicSpotMPO = scenicSpotDao.qyerySpotById(request.getScenicSpotId());
        List<ScenicProductSortMPO> scenicProductSortMPOS = scenicProductSortDao.queryScenicProductSortByScenicId(request.getScenicSpotId());

        String date = request.getDate();
        List<ScenicSpotProductBase> productBases = Lists.newArrayList();
        if(ListUtils.isNotEmpty(scenicSpotProductMPOS)){
            log.info("????????????????????????????????????{}",JSON.toJSONString(scenicSpotProductMPOS));
            for (ScenicSpotProductMPO scenicSpotProduct : scenicSpotProductMPOS) {
                ProductListMPO productListMPO = productDao.getProductByProductId(scenicSpotProduct.getId());
                String productId = scenicSpotProduct.getId();
                //????????????????????????
                int bookBeforeDay = scenicSpotProduct.getScenicSpotProductTransaction() == null ? 0 : scenicSpotProduct.getScenicSpotProductTransaction().getBookBeforeDay();
                Date canBuyDate = getCanBuyDate(bookBeforeDay, scenicSpotProduct.getScenicSpotProductTransaction() == null ? null : scenicSpotProduct.getScenicSpotProductTransaction().getBookBeforeTime());
                List<ScenicSpotProductPriceMPO> scenicSpotProductPriceMPOS = scenicSpotDao.queryProductPriceByProductId(productId);
                // ????????????id?????????id????????? ??????????????????
                Map<String, List<ScenicSpotProductPriceMPO>> priceMap = scenicSpotProductPriceMPOS.stream().collect(Collectors.groupingBy(price ->
                        String.format("%s-%s-%s", price.getScenicSpotProductId(), price.getScenicSpotRuleId(), price.getTicketKind())));
                log.info("??????{}??????{}???", scenicSpotProduct.getId(), priceMap.size());
                priceMap.forEach((k, v) -> {
                    ScenicSpotProductPriceMPO scenicSpotProductPriceMPO = filterPrice(v, date, canBuyDate);
                    if(scenicSpotProductPriceMPO == null){
                        return;
                    }
                    String scenicSpotRuleId = scenicSpotProductPriceMPO.getScenicSpotRuleId();
                    ScenicSpotRuleMPO scenicSpotRuleMPO = scenicSpotDao.queryRuleById(scenicSpotRuleId);
                    if(scenicSpotRuleMPO == null){
                        log.info("????????????????????????????????????????????????,????????????id???{}, ???????????????????????????{}",scenicSpotProduct.getId(), JSON.toJSON(scenicSpotProductPriceMPO));
                        return;
                    }

                    ScenicSpotProductBase scenicSpotProductBase = new ScenicSpotProductBase();
                    String category = scenicSpotProduct.getScenicSpotProductBaseSetting().getCategoryCode();
                    BasePrice basePrice = new BasePrice();
                    BeanUtils.copyProperties(scenicSpotProductPriceMPO,basePrice);
                    //????????????????????????
                    IncreasePrice increasePrice = new IncreasePrice();
                    increasePrice.setChannelCode(scenicSpotProduct.getChannel());
                    increasePrice.setProductCode(scenicSpotProductPriceMPO.getScenicSpotProductId());
                    increasePrice.setAppSource(request.getFrom());
                    increasePrice.setAppSubSource(request.getSource());
                    increasePrice.setProductCategory(category);
                    List<IncreasePriceCalendar> priceCalendars = new ArrayList<>(1);
                    IncreasePriceCalendar priceCalendar = new IncreasePriceCalendar();
                    priceCalendar.setAdtSellPrice(scenicSpotProductPriceMPO.getSellPrice());
                    priceCalendar.setDate(scenicSpotProductPriceMPO.getStartDate());
                    priceCalendar.setAdtFloatPriceManually(scenicSpotProductPriceMPO.isFloatPriceManually());
                    priceCalendar.setAdtFloatPriceType(scenicSpotProductPriceMPO.getFloatPriceType());
                    priceCalendar.setAdtFloatPrice(scenicSpotProductPriceMPO.getFloatPrice());
                    priceCalendar.setAdtSettlePrice(scenicSpotProductPriceMPO.getSettlementPrice());
                    priceCalendars.add(priceCalendar);
                    increasePrice.setPrices(priceCalendars);
                    increasePrice.setScenicSpotId(request.getScenicSpotId());
                    commonService.increasePrice(increasePrice);
                    List<IncreasePriceCalendar> prices = increasePrice.getPrices();
                    if(ListUtils.isEmpty(prices)){
                        log.info("???????????????????????????????????????????????????,????????????id???{}, ???????????????????????????{}",scenicSpotProduct.getId(), JSON.toJSON(scenicSpotProductPriceMPO));
                        return;
                    }
                    BeanUtils.copyProperties(scenicSpotProductPriceMPO,basePrice,"sellPrice");
                    IncreasePriceCalendar increasePriceCalendar = prices.get(0);
                    basePrice.setSellPrice(increasePriceCalendar.getAdtSellPrice());
                    basePrice.setPriceId(scenicSpotProductPriceMPO.getId());
                    if(scenicSpotProductPriceMPO.getMarketPrice() != null && scenicSpotProductPriceMPO.getMarketPrice().compareTo(increasePriceCalendar.getAdtSellPrice()) > 0){
                        basePrice.setOriPrice(scenicSpotProductPriceMPO.getMarketPrice());
                    }
                    scenicSpotProductBase.setPrice(basePrice);

                    BeanUtils.copyProperties(scenicSpotProduct,scenicSpotProductBase);
                    productBases.add(scenicSpotProductBase);
                    scenicSpotProductBase.setCategory(category);
                    scenicSpotProductBase.setTicketKind(scenicSpotProductPriceMPO.getTicketKind());
                    scenicSpotProductBase.setProductId(scenicSpotProduct.getId());
                    scenicSpotProductBase.setSellCount(productListMPO.getSellCount());
                    scenicSpotProductBase.setStock(productListMPO.getStock());
                    scenicSpotProductBase.setChannel(productListMPO.getChannel());
                    scenicSpotProductBase.setSortId(String.format("%s%s%s",scenicSpotProductPriceMPO.getScenicSpotProductId(), scenicSpotProductPriceMPO.getScenicSpotRuleId(), scenicSpotProductPriceMPO.getTicketKind()));

                    //??????????????????????????????
                    String startDate = scenicSpotProductPriceMPO.getStartDate();
                    if (canBuyDate.after(DateTimeUtil.parseDate(startDate))) {
                        startDate = DateTimeUtil.formatDate(canBuyDate);
                    }
                    LocalDate localDate = LocalDate.now();
                    LocalDate tomorrow = localDate.plusDays(1);
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    String dateStr = localDate.format(fmt);
                    String tomorrowStr = tomorrow.format(fmt);
                    List<Tag> bookTag = new ArrayList<>();
                    Tag tag = null;
                    if(StringUtils.equals(startDate,dateStr)) {
                        tag = new Tag();
                        tag.setName("????????????");
                        tag.setColour(ColourConstants.TICKET_BLUE);
                        bookTag.add(tag);
                    }else if(StringUtils.equals(startDate,tomorrowStr)){
                        tag = new Tag();
                        tag.setName("????????????");
                        tag.setColour(ColourConstants.TICKET_BLUE);
                        bookTag.add(tag);
                    }
                    if(StringUtils.isNotBlank(increasePriceCalendar.getTagDesc()) && StringUtils.isNotBlank(increasePriceCalendar.getTagDesc())){
                        Tag discountTag = new Tag();
                        discountTag.setName(String.format("%s%s???", increasePriceCalendar.getTagDesc(), increasePriceCalendar.getTag()));
                        discountTag.setColour(ColourConstants.TICKET_GREEN);
                        bookTag.add(discountTag);
                    }
                    scenicSpotProductBase.setBookTag(bookTag);

                    List<Tag> ticketkind = new ArrayList<>();
                    String refundTag = scenicSpotRuleMPO.getRefundTag();
                    if(StringUtils.isNotEmpty(refundTag)){
                        Tag tag1 = new Tag();
                        if(StringUtils.equals("?????????",refundTag)){
                            tag1.setColour(ColourConstants.TICKET_GREEN);
                        }
                        tag1.setName(refundTag);
                        ticketkind.add(tag1);
                    }
                    int ticketType = scenicSpotRuleMPO.getTicketType();
                    Tag tag1 = new Tag();
                    tag1.setName(0 == ticketType?"?????????":"?????????");
                    ticketkind.add(tag1);

                    int limitBuy = scenicSpotRuleMPO.getLimitBuy();
                    if(1 == limitBuy && scenicSpotRuleMPO.getMaxCount() <= 9){
                        int maxCount = scenicSpotRuleMPO.getMaxCount();
                        Tag tag2 = new Tag();
                        tag2.setName("??????".concat(String.valueOf(maxCount)).concat("???/???"));
                        tag2.setColour(ColourConstants.TICKET_BLUE);
                        ticketkind.add(tag2);
                    }
                    ScenicSpotProductTransaction scenicSpotProductTransaction = scenicSpotProduct.getScenicSpotProductTransaction();
                    if(scenicSpotProductTransaction != null){
                        int ticketOutHour = scenicSpotProductTransaction.getTicketOutHour();
                        int ticketOutMinute = scenicSpotProductTransaction.getTicketOutMinute();
                        if( 0== ticketOutHour &&0 == ticketOutMinute){
                            Tag tag2 = new Tag();
                            tag2.setName("????????????");
                            ticketkind.add(tag2);
                            scenicSpotProductBase.setAnyTime(1);
                        }
                    }
                    scenicSpotProductBase.setTicketkindTag(ticketkind);
                });

            }
        }

        //????????????
        List<ScenicSpotProductBase> result = new ArrayList<>();
        result.addAll(productBases);
        if (!CollectionUtils.isEmpty(scenicProductSortMPOS)){
            Map<String,List<ScenicSpotProductBase>> scenicRealProductMap = result.stream().collect(Collectors.groupingBy(ScenicSpotProductBase::getSortId, LinkedHashMap::new,Collectors.toList()));
            result = new ArrayList<>();
            int sort = 0;
            for (ScenicProductSortMPO scenicProductSortMPO : scenicProductSortMPOS){
                if (!CollectionUtils.isEmpty(scenicRealProductMap.get(scenicProductSortMPO.getId()))) {
                    scenicRealProductMap.get(scenicProductSortMPO.getId()).get(0).setSort(scenicProductSortMPO.getSort());
                    result.add(scenicRealProductMap.get(scenicProductSortMPO.getId()).get(0));
                    scenicRealProductMap.remove(scenicProductSortMPO.getId());
                    sort++;
                }
            }
            for(Map.Entry<String,List<ScenicSpotProductBase>> entry:scenicRealProductMap.entrySet()){
                entry.getValue().get(0).setSort(sort++);
                result.add(entry.getValue().get(0));
            }
        }
        //????????????
        if (StringUtils.isBlank(scenicSpotMPO.getTicketKindSort())){
            //????????????????????????   ??????????????????
            scenicSpotMPO.setTicketKindSort("2,19");
        }
        String [] ticketKindSorts = scenicSpotMPO.getTicketKindSort().split(",");
        Map<String,List<ScenicSpotProductBase>> resultMap = result.stream().collect(Collectors.groupingBy(ScenicSpotProductBase::getTicketKind, LinkedHashMap::new,Collectors.toList()));
        result = new ArrayList<>();
        for (String ticketKindSort : ticketKindSorts){
            if (!CollectionUtils.isEmpty(resultMap.get(ticketKindSort))) {
                result.addAll(resultMap.get(ticketKindSort));
                resultMap.remove(ticketKindSort);
            }
        }
        for(Map.Entry<String,List<ScenicSpotProductBase>> entry:resultMap.entrySet()){
            result.addAll(entry.getValue());
        }
        return BaseResponse.withSuccess(result);
    }

    private Date getCanBuyDate(int bookBeforeDay, String bookBeforeTime) {
        Date current = new Date();
        if (StringUtils.isNotBlank(bookBeforeTime)) {
            if (StringUtils.equals(bookBeforeTime, "00:00")) {
                bookBeforeDay+=1;
            }else{
                if(bookBeforeDay == 0){
                    try {
                        Date bookDateTime = DateTimeUtil.parse(DateTimeUtil.formatDate(current, DateTimeUtil.YYYYMMDD)+" " + bookBeforeTime, DateTimeUtil.YYYYMMDDHHmm);
                        if(bookDateTime.compareTo(current) < 0){
                            bookBeforeDay += 1;
                        }
                    }catch (Exception e){
                        log.error("bookBeforeTime????????????:{}", bookBeforeTime);
                    }
                }
            }
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(current);
        calendar.add(Calendar.DAY_OF_MONTH, bookBeforeDay);
        Date canBuyDate = calendar.getTime();
        return canBuyDate;
    }

    private ScenicSpotProductPriceMPO filterPrice(List<ScenicSpotProductPriceMPO> list, String date, Date canBuyDate){
        if(ListUtils.isEmpty(list)){
            return null;
        }else{
            if(StringUtils.isNotEmpty(date)){
                //List<ScenicSpotProductPriceMPO> collect = list.stream().filter(p -> StringUtils.equals(p.getStartDate(), date)).collect(Collectors.toList());
                //2021-08-05 ???????????????????????????????????????????????????????????????
                Date reqDate = DateTimeUtil.parseDate(date);
                if(DateTimeUtil.getDateDiffDays(reqDate, canBuyDate) < 0){
                    //???????????????????????????????????????????????????
                    return null;
                }
                List<ScenicSpotProductPriceMPO> collect = list.stream().filter(p -> {
                    if (StringUtils.isNotBlank(p.getStartDate())) {
                        Date startDate = DateTimeUtil.parseDate(p.getStartDate());
                        if(startDate.compareTo(reqDate) > 0){
                            return false;
                        }
                    }
                    if (StringUtils.isNotBlank(p.getEndDate())) {
                        Date endDate = DateTimeUtil.parseDate(p.getEndDate());
                        if(endDate.compareTo(reqDate) < 0){
                            return false;
                        }
                    }
                    return true;
                }).collect(Collectors.toList());
                if(ListUtils.isEmpty(collect)){
                    return null;
                }else{
                    String dayOfWeekByDate = getDayOfWeekByDate(date);
                    if(!StringUtils.equals("0",dayOfWeekByDate)){
                        List<ScenicSpotProductPriceMPO> collect1 = collect.stream().filter(pp -> pp.getWeekDay().contains(dayOfWeekByDate)).collect(Collectors.toList());
                        if(ListUtils.isEmpty(collect1)){
                            return null;
                        }else{
                            collect1 = collect1.stream().sorted(Comparator.comparing(ScenicSpotProductPriceMPO::getStartDate)).collect(Collectors.toList());
                            return collect1.get(0);
                        }
                    }
                }
            }else{
                list = list.stream().sorted(Comparator.comparing(ScenicSpotProductPriceMPO::getStartDate)).collect(Collectors.toList());
                return list.get(0);
            }
        }
        return null;
    }



    @Override
    public BaseResponse<List<BasePrice>> queryCalendar(CalendarRequest request) {
        try {
            String scenicSpotId = request.getScenicSpotId();
            String startDate = request.getStartDate();
            Date start = DateTimeUtil.parseDate(startDate);
            String productId = request.getProductId();
            String endDate = request.getEndDate();
            String packageId = request.getPackageId();
            List<BasePrice> basePrices = null;
            List<ScenicSpotProductPriceMPO> effective = new ArrayList<>();

            BaseResponse<List<ChannelInfo>> listBaseResponse = dataService.queryChannelInfo(1);
            List<String> channelInfo = new ArrayList<>();
            if(listBaseResponse.getCode() == 0 && listBaseResponse.getData() != null){
                channelInfo = listBaseResponse.getData().stream().map(a -> a.getChannel()).collect(Collectors.toList());
            }
            if (StringUtils.isEmpty(productId)) {
                List<ScenicSpotProductMPO> scenicSpotProductMPOS = scenicSpotDao.querySpotProduct(scenicSpotId, channelInfo);
                if (ListUtils.isNotEmpty(scenicSpotProductMPOS)) {
                    List<String> productIds = scenicSpotProductMPOS.stream().map(a -> a.getId()).collect(Collectors.toList());
                    List<ScenicSpotProductPriceMPO> priceMPOS = scenicSpotDao.queryPriceByProductIds(productIds, startDate, endDate);
                    Map<String, List<ScenicSpotProductPriceMPO>> priceMapByProductId = priceMPOS.stream().collect(Collectors.groupingBy(a -> a.getScenicSpotProductId()));
                    for (ScenicSpotProductMPO productMPO : scenicSpotProductMPOS) {
                        String productMPOId = productMPO.getId();
                        List<ScenicSpotProductPriceMPO> priceMPOList = priceMapByProductId.get(productMPOId);
                        //????????????????????????
                        int bookBeforeDay = productMPO.getScenicSpotProductTransaction() == null ? 0 : productMPO.getScenicSpotProductTransaction().getBookBeforeDay();
                        Date canBuyDate = getCanBuyDate(bookBeforeDay, productMPO.getScenicSpotProductTransaction() == null ? null : productMPO.getScenicSpotProductTransaction().getBookBeforeTime());
                        String trueStartDate = DateTimeUtil.getDateDiffDays(start, canBuyDate) < 0 ? DateTimeUtil.formatDate(canBuyDate) : startDate;
                        int sellType = productMPO.getSellType();
                        //??????????????????????????? ????????????
                        if (sellType == 0) {
                            //List<ScenicSpotProductPriceMPO> scenicSpotProductPriceMPOS = scenicSpotDao.queryPriceByProductIdAndDate(productMPOId, null, null);
                            for (ScenicSpotProductPriceMPO scenicSpotProductPriceMPO : priceMPOList) {
                                if (StringUtils.isNotBlank(request.getPackageId()) && !scenicSpotProductPriceMPO.getId().equals(request.getPackageId())){
                                    continue;
                                }
                                List<ScenicSpotProductPriceMPO> scenicSpotProductPriceMPOS1 = splitCalendar(scenicSpotProductPriceMPO, trueStartDate, endDate, canBuyDate);
                                if (ListUtils.isNotEmpty(scenicSpotProductMPOS)) {
                                    effective.addAll(scenicSpotProductPriceMPOS1);
                                }
                            }

                        }else{
                            //List<ScenicSpotProductPriceMPO> priceMPOS = scenicSpotDao.queryPriceByProductIdAndDate(productMPOId, trueStartDate, endDate);
                            //effective.addAll(priceMPOS);
                            for (ScenicSpotProductPriceMPO scenicSpotProductPriceMPO : priceMPOS){
                                if (StringUtils.isNotBlank(request.getPackageId()) && !scenicSpotProductPriceMPO.getId().equals(request.getPackageId())){
                                    continue;
                                }
                                Date saleDate = DateTimeUtil.parseDate(scenicSpotProductPriceMPO.getStartDate());
                                if(DateTimeUtil.getDateDiffDays(saleDate, canBuyDate) < 0){
                                    continue;
                                }
                                effective.add(scenicSpotProductPriceMPO);
                            }
                        }

                    }
                }

            }else {
                //????????????????????????
                ScenicSpotProductMPO scenicSpotProductMPO = scenicSpotDao.querySpotProductById(productId, channelInfo);
                scenicSpotId = scenicSpotProductMPO.getScenicSpotId();
                int bookBeforeDay = scenicSpotProductMPO.getScenicSpotProductTransaction() == null ? 0 : scenicSpotProductMPO.getScenicSpotProductTransaction().getBookBeforeDay();
                Date canBuyDate = getCanBuyDate(bookBeforeDay, scenicSpotProductMPO.getScenicSpotProductTransaction() == null ? null : scenicSpotProductMPO.getScenicSpotProductTransaction().getBookBeforeTime());
                String trueStartDate = DateTimeUtil.getDateDiffDays(start, canBuyDate) < 0 ? DateTimeUtil.formatDate(canBuyDate) : startDate;
                List<ScenicSpotProductPriceMPO> scenicSpotProductPriceMPOS = getPrice(productId, packageId, trueStartDate, endDate);
                if (ListUtils.isNotEmpty(scenicSpotProductPriceMPOS)) {
                    log.info("????????????id????????????????????????{}", JSON.toJSONString(scenicSpotProductPriceMPOS));
                    for (ScenicSpotProductPriceMPO s : scenicSpotProductPriceMPOS) {
                        String startDate1 = s.getStartDate();
                        String endDate1 = s.getEndDate();
                        if (!StringUtils.equals(startDate1, endDate1)) {
                            List<ScenicSpotProductPriceMPO> scenicSpotProductPriceMPOS1 = splitCalendar(s, startDate, endDate, canBuyDate);
                            if (ListUtils.isNotEmpty(scenicSpotProductPriceMPOS1)) {
                                effective.addAll(scenicSpotProductPriceMPOS1);
                            }
                        } else {
                            Date saleDate = DateTimeUtil.parseDate(s.getStartDate());
                            if(DateTimeUtil.getDateDiffDays(saleDate, canBuyDate) < 0){
                                continue;
                            }
                            effective.add(s);
                        }
                    }
                }
            }

            if (ListUtils.isNotEmpty(effective)) {
                List<ScenicSpotProductPriceMPO> fe = new ArrayList<>(effective.size());
                for (ScenicSpotProductPriceMPO ss : effective) {
                    String startDate1 = ss.getStartDate();
                    String dayOfWeekByDate = getDayOfWeekByDate(startDate1);
                    String weekDay = ss.getWeekDay();
                    if (StringUtils.isBlank(weekDay)) {
                        weekDay = "1,2,3,4,5,6,7";
                    }
                    if (weekDay.contains(dayOfWeekByDate)) {
                        fe.add(ss);
                    }

                }
                Map<String, List<ScenicSpotProductPriceMPO>> priceMapByDate = fe.stream().collect(Collectors.groupingBy(a -> a.getStartDate()));
                Set<String> dates = priceMapByDate.keySet();
                List<ScenicSpotProductPriceMPO> finalPriceList = new ArrayList<>();
                //????????????????????????
                for (String date : dates) {
                    List<ScenicSpotProductPriceMPO> priceMPOS = priceMapByDate.get(date);
                    priceMPOS.sort(Comparator.comparing(a -> a.getSellPrice()));
                    finalPriceList.add(priceMPOS.get(0));
                }
                effective = finalPriceList.stream().sorted(Comparator.comparing(ScenicSpotProductPriceMPO::getStartDate)).collect(Collectors.toList());
                List<String> finalChannelInfo = channelInfo;
                String finalScenicSpotId = scenicSpotId;
                basePrices = effective.stream().map(p -> {
                    BasePrice basePrice = new BasePrice();
                    BeanUtils.copyProperties(p, basePrice);
                    //????????????????????????
                    IncreasePrice increasePrice = new IncreasePrice();
                    ScenicSpotProductMPO scenicSpotProductMPO = scenicSpotDao.querySpotProductById(p.getScenicSpotProductId(), finalChannelInfo);
                    if(scenicSpotProductMPO != null){
                        increasePrice.setChannelCode(scenicSpotProductMPO.getChannel());
                    }
                    //increasePrice.setChannelCode(request.getChannelCode());
                    increasePrice.setProductCode(p.getScenicSpotProductId());
                    increasePrice.setAppSource(request.getFrom());
                    increasePrice.setAppSubSource(request.getSource());
                    increasePrice.setProductCategory("d_ss_ticket");
                    List<IncreasePriceCalendar> priceCalendars = new ArrayList<>(1);
                    IncreasePriceCalendar priceCalendar = new IncreasePriceCalendar();
                    priceCalendar.setAdtSellPrice(p.getSellPrice());
                    priceCalendar.setDate(p.getStartDate());
                    priceCalendar.setAdtFloatPriceManually(p.isFloatPriceManually());
                    priceCalendar.setAdtFloatPriceType(p.getFloatPriceType());
                    priceCalendar.setAdtFloatPrice(p.getFloatPrice());
                    priceCalendar.setAdtSettlePrice(p.getSettlementPrice());
                    priceCalendars.add(priceCalendar);
                    increasePrice.setPrices(priceCalendars);
                    increasePrice.setScenicSpotId(finalScenicSpotId);
                    commonService.increasePrice(increasePrice);
                    List<IncreasePriceCalendar> prices = increasePrice.getPrices();
                    IncreasePriceCalendar priceCalendar1 = prices.get(0);
                    basePrice.setSellPrice(priceCalendar1.getAdtSellPrice());
                    basePrice.setPriceId(p.getId());
                    basePrice.setStartDate(DateTimeUtil.format(DateTimeUtil.parseDate(p.getStartDate()),DateTimeUtil.YYYYMMDD));
                    return basePrice;
                }).collect(Collectors.toList());
            }
            return BaseResponse.withSuccess(basePrices);
        }catch (Exception e){
            log.error("??????????????????", e);
        }
        return BaseResponse.withSuccess();
    }

    private List<ScenicSpotProductPriceMPO> getPrice(String productId, String packageId, String startDate, String endDate){
        String ruleId = null;
        String ticketKind = null;
        // ???????????????????????????packageId????????????????????????????????????productid?????????
        if(StringUtils.isNotBlank(packageId)){
            ScenicSpotProductPriceMPO priceMPO = scenicSpotProductPriceDao.getPriceById(packageId);
            if(priceMPO != null){
                ruleId = priceMPO.getScenicSpotRuleId();
                ticketKind = priceMPO.getTicketKind();
            }
        }
        return scenicSpotDao.queryPrice(productId, startDate, endDate, ruleId, ticketKind);
    }

    /**
     * ???????????? ????????????????????? ??????
     */
     private static String getDayOfWeekByDate(String date) {
        String dayOfweek = "0";
        String [] weekNum ={"?????????","?????????","?????????","?????????","?????????","?????????","?????????"};
         List<String> list = Arrays.asList(weekNum);
         try {
            SimpleDateFormat myFormatter = new SimpleDateFormat("yyyy-MM-dd");
            Date myDate = myFormatter.parse(date);
            SimpleDateFormat formatter = new SimpleDateFormat("E",Locale.SIMPLIFIED_CHINESE);
            String str = formatter.format(myDate);
            if(list.contains(str)){
                dayOfweek = String.valueOf(list.indexOf(str)+1);
            }
            } catch (Exception e) {
            }
            return dayOfweek;
     }

     private List<ScenicSpotProductPriceMPO> splitCalendar(ScenicSpotProductPriceMPO scenicSpotProductPriceMPO, String startDate, String endDate, Date canBuyDate) {
         List<ScenicSpotProductPriceMPO> list = null;
         try {
             String sdate = scenicSpotProductPriceMPO.getStartDate();
             String edate = scenicSpotProductPriceMPO.getEndDate();
             SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
             Date dBegin = simpleDateFormat.parse(sdate);// ??????????????????
             Date dEnd = simpleDateFormat.parse(edate);// ??????????????????

             List<ScenicSpotProductPriceMPO> lDate = new ArrayList();
             ScenicSpotProductPriceMPO st = new ScenicSpotProductPriceMPO();
             BeanUtils.copyProperties(scenicSpotProductPriceMPO,st,"startDate","endDate");
             st.setStartDate(startDate);
             st.setEndDate(startDate);
             lDate.add(st);

             Calendar calBegin = Calendar.getInstance();
             // ??????????????? Date ????????? Calendar ?????????
             calBegin.setTime(dBegin);
             Calendar calEnd = Calendar.getInstance();
             // ??????????????? Date ????????? Calendar ?????????
             calEnd.setTime(dEnd);
             // ??????????????????????????????????????????
             while (dEnd.compareTo(calBegin.getTime())>= 0) {
                 // ?????????????????????????????????????????????????????????????????????????????????
                 if(DateTimeUtil.getDateDiffDays(calBegin.getTime(), canBuyDate) < 0){
                     continue;
                 }
                 calBegin.add(Calendar.DAY_OF_MONTH, 1);
                 ScenicSpotProductPriceMPO st1 = new ScenicSpotProductPriceMPO();
                 BeanUtils.copyProperties(scenicSpotProductPriceMPO,st1,"startDate","endDate");
                 st1.setStartDate(simpleDateFormat.format(calBegin.getTime()));
                 st1.setEndDate(simpleDateFormat.format(calBegin.getTime()));
                 lDate.add(st1);
             }

             if(ListUtils.isNotEmpty(lDate)) {
                 if (StringUtils.isNotEmpty(startDate) && StringUtils.isNotEmpty(endDate)) {
                     list = lDate.stream().filter(p -> p.getStartDate().compareTo(startDate) >= 0 && p.getStartDate().compareTo(endDate) <= 0).collect(Collectors.toList());
                 } else {
                     if (StringUtils.isNotEmpty(startDate)) {
                         list = lDate.stream().filter(p -> p.getStartDate().compareTo(startDate) >= 0).collect(Collectors.toList());
                     }
                     if (StringUtils.isNotEmpty(endDate)) {
                         list = lDate.stream().filter(p -> p.getStartDate().compareTo(startDate) <= 0).collect(Collectors.toList());
                     }
                 }
             }
         } catch (Exception e) {

         }
         return list;
     }


     /**
      * @author: wangying
      * @date 5/14/21 11:52 AM
      * ?????????????????????
      * [request]
      * @return {@link BaseResponse< ProductPriceCalendarResult>}
      * @throws
      */
    @Override
    public BaseResponse<ProductPriceCalendarResult> queryGroupTourPriceCalendar(CalendarRequest request) {

        ProductPriceCalendarResult result = new ProductPriceCalendarResult();

        BaseResponse<List<ChannelInfo>> listBaseResponse = dataService.queryChannelInfo(1);
        List<String> channelInfo = new ArrayList<>();
        if(listBaseResponse.getCode() == 0 && listBaseResponse.getData() != null){
            channelInfo = listBaseResponse.getData().stream().map(a -> a.getChannel()).collect(Collectors.toList());
        }
        GroupTourProductMPO groupTourProductMPO = groupTourDao.queryTourProduct(request.getProductId(), channelInfo);
        GroupTourProductSetMealMPO groupTourProductSetMealMPO = groupTourDao.queryGroupSetMealBySetId(request.getSetMealId());
        List<GroupTourPrice> groupTourPrices = groupTourProductSetMealMPO.getGroupTourPrices();
        int beforeBookDay = groupTourProductMPO.getGroupTourProductPayInfo().getBeforeBookDay();
        //????????????
        Date startDate = new Date();
        if(StringUtils.isNotBlank(request.getStartDate())){
            Date reqStartDate = DateTimeUtil.parse(request.getStartDate(), DateTimeUtil.YYYYMMDD);
            if(DateTimeUtil.getDateDiffDays(reqStartDate, startDate) > 0) {
                startDate = reqStartDate;
            }
        }
        //???????????????????????????????????????????????????
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.add(Calendar.DAY_OF_MONTH, beforeBookDay);
        startDate = calendar.getTime();
        Date endDate = null;
        if(StringUtils.isNotBlank(request.getEndDate())){
            Date reqEndDate = DateTimeUtil.parse(request.getEndDate(), DateTimeUtil.YYYYMMDD);
            if(DateTimeUtil.getDateDiffDays(reqEndDate, startDate)> 0) {
                endDate = reqEndDate;
            }
        }
        final Date searchStartDate = startDate;
        final Date searchEndDate = endDate;
        groupTourPrices = groupTourPrices.stream().filter(item -> {
            Date saleDate = DateTimeUtil.parse(item.getDate(), DateTimeUtil.YYYYMMDD);
            if(DateTimeUtil.getDateDiffDays(saleDate, searchStartDate) >= 0){
                if(searchEndDate == null || (searchEndDate != null && DateTimeUtil.getDateDiffDays(saleDate, searchEndDate) <= 0)){
                    return true;
                }else{
                    return false;
                }
            }else{
                return false;
            }
        }).collect(Collectors.toList());
        if(CollectionUtils.isNotEmpty(groupTourPrices)){
            //????????????
            IncreasePrice increasePrice = new IncreasePrice();
            increasePrice.setChannelCode(groupTourProductMPO.getChannel());
            increasePrice.setProductCode(groupTourProductMPO.getId());
            increasePrice.setAppSource(request.getFrom());
            increasePrice.setAppSubSource(request.getSource());
            increasePrice.setProductCategory(groupTourProductMPO.getCategory());
            increasePrice.setAppSubSource(request.getSource());
            List<IncreasePriceCalendar> priceCalendars = groupTourPrices.stream().map(item -> {
               IncreasePriceCalendar priceCalendar = new IncreasePriceCalendar();
                priceCalendar.setAdtSellPrice(item.getAdtSellPrice());
                priceCalendar.setChdSellPrice(item.getChdSellPrice());
                priceCalendar.setDate(item.getDate());

                priceCalendar.setAdtSettlePrice(item.getAdtPrice());
                priceCalendar.setAdtFloatPriceType(item.getAdtFloatPriceType());
                priceCalendar.setAdtFloatPrice(item.getAdtFloatPrice());
                priceCalendar.setAdtFloatPriceManually(item.isAdtFloatPriceManually());

                priceCalendar.setChdFloatPrice(item.getChdFloatPrice());
                priceCalendar.setChdFloatPriceType(item.getChdFloatPriceType());
                priceCalendar.setChdSettlePrice(item.getChdPrice());
                priceCalendar.setChdFloatPriceManually(item.isChdFloatPriceManually());
               return priceCalendar;
            }).collect(Collectors.toList());
            increasePrice.setPrices(priceCalendars);
            commonService.increasePrice(increasePrice);

            Map<String, GroupTourPrice> priceMap = Maps.uniqueIndex(groupTourPrices, a -> a.getDate());

            List<PriceInfo> resultPrices = increasePrice.getPrices().stream().map(item -> {
                PriceInfo priceInfo = new PriceInfo();
                GroupTourPrice groupTourPrice = priceMap.get(item.getDate());
                priceInfo.setProductCode(request.getProductId());
                priceInfo.setSaleDate(item.getDate());
                priceInfo.setSalePrice(item.getAdtSellPrice());
                priceInfo.setSettlePrice(groupTourPrice.getAdtPrice());
                priceInfo.setStock(groupTourPrice.getAdtStock());
                priceInfo.setChdSalePrice(item.getChdSellPrice());
                priceInfo.setChdSettlePrice(groupTourPrice.getChdPrice());
                return priceInfo;
            }).collect(Collectors.toList());
            result.setPriceInfos(resultPrices);
        }

        return BaseResponse.withSuccess(result);
    }

    @Override
    public BaseResponse<ScenicSpotProductDetail> queryScenicSpotProductDetail(ScenicSpotProductRequest request) {

        List<String> channelInfo = new ArrayList<>();
        if (!StringUtils.equals(request.getFrom(), "order")) {
            BaseResponse<List<ChannelInfo>> listBaseResponse = dataService.queryChannelInfo(1);
            if(listBaseResponse.getCode() == 0 && listBaseResponse.getData() != null){
                channelInfo = listBaseResponse.getData().stream().map(a -> a.getChannel()).collect(Collectors.toList());
            }
        }
        ScenicSpotProductDetail scenucSpotProductDetail =null;
        ScenicSpotProductMPO scenicSpotProductMPO = scenicSpotDao.querySpotProductById(request.getProductId(), channelInfo);
        ScenicSpotProductPriceMPO scenicSpotProductPriceMPO = scenicSpotDao.querySpotProductPriceById(request.getPriceId());
        if(scenicSpotProductMPO != null) {
            scenucSpotProductDetail =new ScenicSpotProductDetail();
            BeanUtils.copyProperties(scenicSpotProductMPO, scenucSpotProductDetail);
        }
        if(scenicSpotProductPriceMPO != null) {
            String scenicSpotRuleId = scenicSpotProductPriceMPO.getScenicSpotRuleId();
            ScenicSpotRuleMPO scenicSpotRuleMPO = scenicSpotDao.queryRuleById(scenicSpotRuleId);
            if(scenucSpotProductDetail == null){
                scenucSpotProductDetail =new ScenicSpotProductDetail();
            }
            BasePrice basePrice = new BasePrice();
            BeanUtils.copyProperties(scenicSpotProductPriceMPO, basePrice);
            basePrice.setPriceId(scenicSpotProductPriceMPO.getId());
            scenucSpotProductDetail.setPrice(basePrice);

            if(scenicSpotRuleMPO != null){
                ScenicSpotProductPriceRuleBase ruleBase = new ScenicSpotProductPriceRuleBase();
                BeanUtils.copyProperties(scenicSpotRuleMPO,ruleBase);
                scenucSpotProductDetail.setRuleBase(ruleBase);

            }
        }

        return BaseResponse.withSuccess(scenucSpotProductDetail);
    }

    /**
     * @author: wangying
     * @date 5/25/21 4:48 PM
     * ???????????????????????? ??????????????????????????????????????????
     * [request]
     * @return {@link BaseResponse< HotelScenicProductDetail>}
     * @throws
     */
    @Override
    public BaseResponse<HotelScenicProductDetail> hotelScenicProductDetail(HotelScenicProductRequest request) {

        List<String> channelInfo = new ArrayList<>();
        if(!StringUtils.equals(request.getFrom(), "order")){
            BaseResponse<List<ChannelInfo>> listBaseResponse = dataService.queryChannelInfo(1);
            if(listBaseResponse.getCode() == 0 && listBaseResponse.getData() != null){
                channelInfo = listBaseResponse.getData().stream().map(a -> a.getChannel()).collect(Collectors.toList());
            }
        }
        HotelScenicSpotProductMPO hotelScenicSpotProductMPO = hotelScenicDao.queryHotelScenicProductMpoById(request.getProductId(), channelInfo);

        HotelScenicProductDetail hotelScenicProductDetail = new HotelScenicProductDetail();
        BeanUtils.copyProperties(hotelScenicSpotProductMPO, hotelScenicProductDetail);
        hotelScenicProductDetail.setProductId(hotelScenicSpotProductMPO.getId());
        hotelScenicProductDetail.setDesc(hotelScenicSpotProductMPO.getComputerDesc());
        return BaseResponse.withSuccess(hotelScenicProductDetail);
    }

    /**
     * @author: wangying
     * @date 5/26/21 10:01 AM
     * ????????????
     * [request]
     * @return {@link BaseResponse< List< HotelScenicProductSetMealBrief>>}
     * @throws
     */
    @Override
    public BaseResponse<List<HotelScenicProductSetMealBrief>> hotelScenicProductSetMealList(CalendarRequest request) {
        BaseResponse<List<ChannelInfo>> listBaseResponse = dataService.queryChannelInfo(1);
        List<String> channelInfo = new ArrayList<>();
        if(listBaseResponse.getCode() == 0 && listBaseResponse.getData() != null){
            channelInfo = listBaseResponse.getData().stream().map(a -> a.getChannel()).collect(Collectors.toList());
        }
        HotelScenicSpotProductMPO productMPO = hotelScenicDao.queryHotelScenicProductMpoById(request.getProductId(), channelInfo);
        Date canBuyDate = getCanBuyDate(productMPO.getPayInfo().getBeforeBookDay(), productMPO.getPayInfo().getLatestBookTime());
        List<HotelScenicProductSetMealBrief> briefList = new ArrayList<>();
        if(productMPO == null){
            return BaseResponse.withSuccess();
        }
        List<HotelScenicSpotProductSetMealMPO> setMealMpoList = hotelScenicDao.queryHotelScenicSetMealList(request);
        if(CollectionUtils.isNotEmpty(setMealMpoList)){
            briefList = setMealMpoList.stream().map(setMealMpo -> {
                HotelScenicProductSetMealBrief brief = new HotelScenicProductSetMealBrief();
                BeanUtils.copyProperties(setMealMpo, brief);
                brief.setPackageId(setMealMpo.getId());
                brief.setProductId(setMealMpo.getHotelScenicSpotProductId());
                brief.setTitle(setMealMpo.getName());
                brief.setThemeElements(productMPO.getPayInfo() == null ? 0 : productMPO.getPayInfo().getThemeElements());
                //????????????
                IncreasePrice increasePrice = hotelIncreasePrice(productMPO, request, setMealMpo.getPriceStocks());
                log.info("??????????????????"+ JSON.toJSONString(increasePrice));
                brief.setPrice(increasePrice.getPrices().stream().filter(a -> {
                    Date saleDate = DateTimeUtil.parseDate(a.getDate());
                    if(DateTimeUtil.getDateDiffDays(saleDate, canBuyDate) < 0){
                        return false;
                    }
                    return StringUtils.isBlank(request.getStartDate()) ? true : StringUtils.equals(a.getDate(), request.getStartDate());
                }).collect(Collectors.toList()).get(0).getAdtSellPrice());
                return brief;
            }).collect(Collectors.toList());
        }
        return BaseResponse.withSuccess(briefList);
    }

    private IncreasePrice hotelIncreasePrice(HotelScenicSpotProductMPO productMPO, CalendarRequest request, List<HotelScenicSpotPriceStock> priceStocks) {
        IncreasePrice increasePrice = new IncreasePrice();
        increasePrice.setChannelCode(productMPO.getChannel());
        increasePrice.setProductCode(productMPO.getId());
        increasePrice.setAppSource(request.getFrom());
        increasePrice.setAppSubSource(request.getSource());
        increasePrice.setProductCategory(productMPO.getCategory());
        List<IncreasePriceCalendar> priceCalendars = priceStocks.stream().map(item -> {
            IncreasePriceCalendar priceCalendar = new IncreasePriceCalendar();
            priceCalendar.setAdtSellPrice(item.getAdtSellPrice());
            priceCalendar.setChdSellPrice(item.getChdSellPrice());
            priceCalendar.setDate(item.getDate());

            priceCalendar.setAdtSettlePrice(item.getAdtPrice());
            priceCalendar.setAdtFloatPriceType(item.getAdtFloatPriceType());
            priceCalendar.setAdtFloatPrice(item.getAdtFloatPrice());
            priceCalendar.setAdtFloatPriceManually(item.isAdtFloatPriceManually());

            priceCalendar.setChdFloatPrice(item.getChdFloatPrice());
            priceCalendar.setChdFloatPriceType(item.getChdFloatPriceType());
            priceCalendar.setChdSettlePrice(item.getChdPrice());
            priceCalendar.setChdFloatPriceManually(item.isChdFloatPriceManually());
            return priceCalendar;
        }).collect(Collectors.toList());
        increasePrice.setPrices(priceCalendars);
        commonService.increasePrice(increasePrice);
        return increasePrice;
    }

    /**
     * @author: wangying
     * @date 5/26/21 2:18 PM
     * ????????????????????????
     * [request]
     * @return {@link BaseResponse< ProductPriceCalendarResult>}
     * @throws
     */
    @Override
    public BaseResponse<ProductPriceCalendarResult> queryHotelScenicPriceCalendar(CalendarRequest request) {
        BaseResponse<List<ChannelInfo>> listBaseResponse = dataService.queryChannelInfo(1);
        List<String> channelInfo = new ArrayList<>();
        if(listBaseResponse.getCode() == 0 && listBaseResponse.getData() != null){
            channelInfo = listBaseResponse.getData().stream().map(a -> a.getChannel()).collect(Collectors.toList());
        }
        ProductPriceCalendarResult result = new ProductPriceCalendarResult();
        HotelScenicSpotProductMPO productMPO = hotelScenicDao.queryHotelScenicProductMpoById(request.getProductId(), channelInfo);
        if(productMPO == null){
            return BaseResponse.withSuccess();
        }
        List<HotelScenicSpotProductSetMealMPO> setMealMpoList = hotelScenicDao.queryHotelScenicSetMealList(request);
        List<HotelScenicSpotPriceStock> allPriceStocks = new ArrayList<>();
        Date canBuyDate = getCanBuyDate(productMPO.getPayInfo().getBeforeBookDay(), productMPO.getPayInfo().getLatestBookTime());
        //??????????????????????????????
        for (HotelScenicSpotProductSetMealMPO hotelScenicSpotProductSetMealMPO : setMealMpoList) {
            allPriceStocks.addAll(hotelScenicSpotProductSetMealMPO.getPriceStocks());
        }
        HotelScenicSpotProductPayInfo payInfo = productMPO.getPayInfo();
        if(payInfo != null && payInfo.getSellType() == 0){
            //??????????????????????????????
            List<HotelScenicSpotPriceStock> tmpPrices = new ArrayList<>();
            for (HotelScenicSpotPriceStock allPriceStock : allPriceStocks) {
                List<HotelScenicSpotPriceStock> splitPriceStock = splitHotelScenicProductCalendar(allPriceStock, payInfo);
                if (CollectionUtils.isNotEmpty(splitPriceStock)) {
                    tmpPrices.addAll(splitPriceStock);
                }
            }
            allPriceStocks = tmpPrices;
        }
        if (CollectionUtils.isEmpty(allPriceStocks)) {
            //??????????????????????????????
            return BaseResponse.withSuccess(result);
        }
        //????????????????????????
        Map<String, List<HotelScenicSpotPriceStock>> priceMapByDate = allPriceStocks.stream().collect(Collectors.groupingBy(a -> a.getDate()));
        Set<String> dates = priceMapByDate.keySet();
        List<HotelScenicSpotPriceStock> priceStocks = new ArrayList<>();
        Date startDate = new Date();
        if(StringUtils.isNotBlank(request.getStartDate())){
            Date reqStartDate = DateTimeUtil.parse(request.getStartDate(), DateTimeUtil.YYYYMMDD);
            if(DateTimeUtil.getDateDiffDays(reqStartDate, startDate) > 0) {
                startDate = reqStartDate;
            }
        }
        Date endDate = null;
        if(StringUtils.isNotBlank(request.getEndDate())){
            Date reqEndDate = DateTimeUtil.parse(request.getEndDate(), DateTimeUtil.YYYYMMDD);
            if(DateTimeUtil.getDateDiffDays(reqEndDate, startDate)> 0) {
                endDate = reqEndDate;
            }
        }
        //??????????????????????????????????????????
        for (String date : dates) {
            Date calendarDate = DateTimeUtil.parse(date, DateTimeUtil.YYYYMMDD);
            if(DateTimeUtil.getDateDiffDays(calendarDate, canBuyDate) < 0){
                continue;
            }
            if(DateTimeUtil.getDateDiffDays(calendarDate, startDate) < 0 || DateTimeUtil.getDateDiffDays(calendarDate, endDate) > 0){
                continue;
            }
            List<HotelScenicSpotPriceStock> tmpPriceStock = priceMapByDate.get(date);
            tmpPriceStock.sort(Comparator.comparing(a -> a.getAdtPrice()));
            priceStocks.add(tmpPriceStock.get(0));
        }
        Map<String, HotelScenicSpotPriceStock> priceStocksByDate = Maps.uniqueIndex(priceStocks, a -> a.getDate());
        //??????
        IncreasePrice increasePrice = hotelIncreasePrice(productMPO, request, priceStocks);
        List<PriceInfo> resultPrices = increasePrice.getPrices().stream().map(item -> {
            PriceInfo priceInfo = new PriceInfo();
            HotelScenicSpotPriceStock hotelScenicSpotPriceStock = priceStocksByDate.get(item.getDate());
            priceInfo.setProductCode(request.getProductId());
            priceInfo.setSaleDate(item.getDate());
            priceInfo.setSalePrice(item.getAdtSellPrice());
            priceInfo.setSettlePrice(hotelScenicSpotPriceStock.getAdtPrice());
            priceInfo.setStock(hotelScenicSpotPriceStock.getAdtStock());
            priceInfo.setChdSalePrice(item.getChdSellPrice());
            priceInfo.setChdSettlePrice(hotelScenicSpotPriceStock.getChdPrice());
            return priceInfo;
        }).collect(Collectors.toList());

        resultPrices = resultPrices.stream().sorted(Comparator.comparing(PriceInfo :: getSaleDate)).collect(Collectors.toList());
        result.setPriceInfos(resultPrices);

        return BaseResponse.withSuccess(result);
    }

    private List<HotelScenicSpotPriceStock> splitHotelScenicProductCalendar(HotelScenicSpotPriceStock allPriceStock, HotelScenicSpotProductPayInfo payInfo) {
        List<HotelScenicSpotPriceStock> list = null;
        try {
            String useStartDate = payInfo.getGoDate().getStartDate();
            String useEndDate = payInfo.getGoDate().getEndDate();
            Date useStart = DateTimeUtil.parse(useStartDate, DateTimeUtil.YYYYMMDD);
            Date useEnd = DateTimeUtil.parseDate(useEndDate, DateTimeUtil.YYYYMMDD);

            Calendar calBegin = Calendar.getInstance();
            calBegin.setTime(useStart);
            while (useEnd.compareTo(calBegin.getTime()) >= 0) {
                String date = DateTimeUtil.formatDate(calBegin.getTime(), DateTimeUtil.YYYYMMDD);
                boolean canSell = true;
                if (CollectionUtils.isNotEmpty(payInfo.getExUseDate())) {
                    for (PeriodDate periodDate : payInfo.getExUseDate()) {
                        Date exStart = DateTimeUtil.parse(periodDate.getStartDate(), DateTimeUtil.YYYYMMDD);
                        Date exEnd = DateTimeUtil.parse(periodDate.getEndDate(), DateTimeUtil.YYYYMMDD);
                        if (exStart.compareTo(calBegin.getTime()) <= 0 && exEnd.compareTo(calBegin.getTime()) >= 0) {
                            //????????????????????????????????????
                            canSell = false;
                            break;
                        }
                    }
                }
                if (canSell && StringUtils.isNotEmpty(payInfo.getExUseWeek())) {
                    //??????????????????????????????
                    String dayOfWeekByDate = getDayOfWeekByDate(date);
                    if (StringUtils.contains(payInfo.getExUseWeek(), dayOfWeekByDate)) {
                        //????????????????????????
                        canSell = false;
                    }
                }
                if(canSell){
                    //?????????????????????
                    HotelScenicSpotPriceStock stock = new HotelScenicSpotPriceStock();
                    BeanUtils.copyProperties(allPriceStock, stock);
                    stock.setDate(date);
                    list.add(stock);
                }
                calBegin.add(Calendar.DAY_OF_MONTH, 1);
            }
        }catch (Exception e){

        }
        return list;
    }

    /**
     * @author: wangying
     * @date 5/26/21 4:17 PM
     * ??????????????????
     * [request]
     * @return {@link BaseResponse< HotelScenicProductSetMealDetail>}
     * @throws
     */
    @Override
    public BaseResponse<HotelScenicProductSetMealDetail> queryHotelScenicSetMealDetail(HotelScenicSetMealRequest request) {

        List<String> channelInfo = new ArrayList<>();
        if (!StringUtils.equals(request.getFrom(), "order")) {
            BaseResponse<List<ChannelInfo>> listBaseResponse = dataService.queryChannelInfo(1);
            if(listBaseResponse.getCode() == 0 && listBaseResponse.getData() != null){
                channelInfo = listBaseResponse.getData().stream().map(a -> a.getChannel()).collect(Collectors.toList());
            }
        }
        HotelScenicSpotProductMPO productMPO = hotelScenicDao.queryHotelScenicProductMpoById(request.getProductId(), channelInfo);
        if(productMPO == null){
            return BaseResponse.withSuccess();
        }
        HotelScenicSpotProductSetMealMPO setMealMPO = hotelScenicDao.queryHotelScenicSetMealById(request);
        HotelScenicProductSetMealDetail result = new HotelScenicProductSetMealDetail();
        BeanUtils.copyProperties(setMealMPO, result);
        result.setProductId(setMealMPO.getHotelScenicSpotProductId());
        result.setPackageId(setMealMPO.getId());
        result.setTitle(setMealMPO.getName());
        HotelScenicSpotProductPayInfo payInfo = productMPO.getPayInfo();
        if(payInfo != null){
            result.setBookNotice(payInfo.getBookNotice());
            result.setCostExclude(payInfo.getCostExclude());
            result.setBookNotices(payInfo.getBookNotices());
            result.setThemeElements(payInfo.getThemeElements());
        }
        result.setChannel(productMPO.getChannel());
        result.setRefundRules(productMPO.getRefundRules());
        return BaseResponse.withSuccess(result);
    }

    /**
     * @author: wangying
     * @date 5/27/21 3:36 PM
     * ????????????
     * [request]
     * @return {@link BaseResponse< HotelInfoBase>}
     * @throws
     */
    @Override
    public BaseResponse<HotelInfoBase> queryHotelInfoDetail(HotelInfoRequest request) {
        HotelMPO hotelMPO = hotelScenicDao.queryHotelMpo(request.getHotelId());
        HotelInfoBase result = new HotelInfoBase();
        if(hotelMPO != null){
            BeanUtils.copyProperties(hotelMPO, result);
        }
        return BaseResponse.withSuccess(result);
    }

    @Override
    public BaseResponse<List<ScenicRealProductBase>> querySpotRelaProductList(ScenicSpotProductRequest request) {
        BaseResponse<List<ChannelInfo>> listBaseResponse = dataService.queryChannelInfo(1);
        List<String> channelInfo = new ArrayList<>();
        if(listBaseResponse.getCode() == 0 && listBaseResponse.getData() != null){
            channelInfo = listBaseResponse.getData().stream().map(a -> a.getChannel()).collect(Collectors.toList());
        }
        log.info("channelInfo = {}",channelInfo);
        List<ScenicSpotProductMPO> scenicSpotProductMPOS = scenicSpotDao.querySpotProduct(request.getScenicSpotId(), channelInfo);
        ScenicSpotMPO scenicSpotMPO = scenicSpotDao.qyerySpotById(request.getScenicSpotId());
        List<ScenicProductSortMPO> scenicProductSortMPOS = scenicProductSortDao.queryScenicProductSortByScenicId(request.getScenicSpotId());

        String date = request.getDate();
        List<ScenicRealProductBase> productBases = Lists.newArrayList();
        if(ListUtils.isNotEmpty(scenicSpotProductMPOS)){
            log.info("????????????????????????????????????{}",JSON.toJSONString(scenicSpotProductMPOS));
            for (ScenicSpotProductMPO scenicSpotProduct : scenicSpotProductMPOS) {
                ProductListMPO productListMPO = productDao.getProductByProductId(scenicSpotProduct.getId());
                String productId = scenicSpotProduct.getId();
                //????????????????????????
                int bookBeforeDay = scenicSpotProduct.getScenicSpotProductTransaction() == null ? 0 : scenicSpotProduct.getScenicSpotProductTransaction().getBookBeforeDay();
                Date canBuyDate = getCanBuyDate(bookBeforeDay, scenicSpotProduct.getScenicSpotProductTransaction() == null ? null : scenicSpotProduct.getScenicSpotProductTransaction().getBookBeforeTime());
                List<ScenicSpotProductPriceMPO> scenicSpotProductPriceMPOS = scenicSpotDao.queryProductPriceByProductId(productId);
                // ????????????id?????????id????????? ??????????????????
                Map<String, List<ScenicSpotProductPriceMPO>> priceMap = scenicSpotProductPriceMPOS.stream().collect(Collectors.groupingBy(price ->
                        String.format("%s-%s-%s", price.getScenicSpotProductId(), price.getScenicSpotRuleId(), price.getTicketKind())));
                log.info("??????{}??????{}???", scenicSpotProduct.getId(), priceMap.size());
                priceMap.forEach((k, v) -> {
                    ScenicSpotProductPriceMPO scenicSpotProductPriceMPO = filterPrice(v, date, canBuyDate);
                    if(scenicSpotProductPriceMPO == null){
                        return;
                    }
                    String scenicSpotRuleId = scenicSpotProductPriceMPO.getScenicSpotRuleId();
                    ScenicSpotRuleMPO scenicSpotRuleMPO = scenicSpotDao.queryRuleById(scenicSpotRuleId);
                    if(scenicSpotRuleMPO == null){
                        log.info("????????????????????????????????????????????????,????????????id???{}, ???????????????????????????{}",scenicSpotProduct.getId(), JSON.toJSON(scenicSpotProductPriceMPO));
                        return;
                    }
                    ScenicRealProductBase scenicSpotProductBase = new ScenicRealProductBase();
                    String category = scenicSpotProduct.getScenicSpotProductBaseSetting().getCategoryCode();
                    BasePrice basePrice = new BasePrice();
                    BeanUtils.copyProperties(scenicSpotProductPriceMPO,basePrice);
                    //????????????????????????
                    IncreasePrice increasePrice = new IncreasePrice();
                    increasePrice.setChannelCode(scenicSpotProduct.getChannel());
                    increasePrice.setProductCode(scenicSpotProductPriceMPO.getScenicSpotProductId());
                    increasePrice.setAppSource(request.getFrom());
                    increasePrice.setAppSubSource(request.getSource());
                    increasePrice.setProductCategory(category);
                    List<IncreasePriceCalendar> priceCalendars = new ArrayList<>(1);
                    IncreasePriceCalendar priceCalendar = new IncreasePriceCalendar();
                    priceCalendar.setAdtSellPrice(scenicSpotProductPriceMPO.getSellPrice());
                    priceCalendar.setDate(scenicSpotProductPriceMPO.getStartDate());
                    priceCalendars.add(priceCalendar);
                    increasePrice.setPrices(priceCalendars);
                    increasePrice.setScenicSpotId(request.getScenicSpotId());
                    commonService.increasePrice(increasePrice);
                    List<IncreasePriceCalendar> prices = increasePrice.getPrices();
                    if(ListUtils.isEmpty(prices)){
                        log.info("???????????????????????????????????????????????????,????????????id???{}, ???????????????????????????{}",scenicSpotProduct.getId(), JSON.toJSON(scenicSpotProductPriceMPO));
                        return;
                    }
                    BeanUtils.copyProperties(scenicSpotProductPriceMPO,basePrice,"sellPrice");
                    IncreasePriceCalendar increasePriceCalendar = prices.get(0);
                    basePrice.setSellPrice(increasePriceCalendar.getAdtSellPrice());
                    basePrice.setPriceId(scenicSpotProductPriceMPO.getId());
                    scenicSpotProductBase.setPrice(basePrice.getSellPrice());

                    BeanUtils.copyProperties(scenicSpotProduct,scenicSpotProductBase);
                    productBases.add(scenicSpotProductBase);
                    scenicSpotProductBase.setCategory(category);
                    scenicSpotProductBase.setProductName(scenicSpotProduct.getName());
                    scenicSpotProductBase.setTicketKind(scenicSpotProductPriceMPO.getTicketKind());
                    scenicSpotProductBase.setProductId(scenicSpotProduct.getId());
                    scenicSpotProductBase.setSellCount(productListMPO.getSellCount());
                    scenicSpotProductBase.setStock(productListMPO.getStock());
                    scenicSpotProductBase.setChannel(productListMPO.getChannel());
                    scenicSpotProductBase.setSortId(String.format("%s%s%s",scenicSpotProductPriceMPO.getScenicSpotProductId(), scenicSpotProductPriceMPO.getScenicSpotRuleId(), scenicSpotProductPriceMPO.getTicketKind()));
                });
            }
        }
        //????????????
        List<ScenicRealProductBase> result = new ArrayList<>();
        result.addAll(productBases);
        if (!CollectionUtils.isEmpty(scenicProductSortMPOS)){
            Map<String,List<ScenicRealProductBase>> scenicRealProductMap = result.stream().collect(Collectors.groupingBy(ScenicRealProductBase::getSortId, LinkedHashMap::new,Collectors.toList()));
            result = new ArrayList<>();
            int sort = 0;
            for (ScenicProductSortMPO scenicProductSortMPO : scenicProductSortMPOS){
                if (!CollectionUtils.isEmpty(scenicRealProductMap.get(scenicProductSortMPO.getId()))) {
                    scenicRealProductMap.get(scenicProductSortMPO.getId()).get(0).setSort(scenicProductSortMPO.getSort());
                    result.add(scenicRealProductMap.get(scenicProductSortMPO.getId()).get(0));
                    scenicRealProductMap.remove(scenicProductSortMPO.getId());
                    sort++;
                }
            }
            for(Map.Entry<String,List<ScenicRealProductBase>> entry:scenicRealProductMap.entrySet()){
                entry.getValue().get(0).setSort(sort++);
                result.add(entry.getValue().get(0));
            }
        }
        //????????????
        if (StringUtils.isNotBlank(scenicSpotMPO.getTicketKindSort())){
            String [] ticketKindSorts = scenicSpotMPO.getTicketKindSort().split(",");
            Map<String,List<ScenicRealProductBase>> resultMap = result.stream().collect(Collectors.groupingBy(ScenicRealProductBase::getTicketKind, LinkedHashMap::new,Collectors.toList()));
            result = new ArrayList<>();
            for (String ticketKindSort : ticketKindSorts){
                if (!CollectionUtils.isEmpty(resultMap.get(ticketKindSort))) {
                    result.addAll(resultMap.get(ticketKindSort));
                    resultMap.remove(ticketKindSort);
                }
            }
            for(Map.Entry<String,List<ScenicRealProductBase>> entry:resultMap.entrySet()){
                result.addAll(entry.getValue());
            }
        }
        return BaseResponse.withSuccess(result);
    }
}
