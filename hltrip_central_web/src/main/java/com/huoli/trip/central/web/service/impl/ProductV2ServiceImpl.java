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
import com.huoli.trip.common.entity.mpo.ProductListMPO;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
    private ScenicSpotProductPriceDao scenicSpotProductPriceDao;

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
     * 跟团游产品详情
     * [request]
     * @return {@link BaseResponse< GroupTourBody>}
     * @throws
     */
    @Override
    public BaseResponse<GroupTourBody> queryGroupTourById(GroupTourRequest request) {
        GroupTourBody groupTourProductBody = null;
        final GroupTourProductMPO groupTourProductMPO = groupTourDao.queryTourProduct(request.getGroupTourId());
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
        log.info("查询到的套餐列表：{}", JSONObject.toJSONString(groupTourProductSetMealMPOS));
        if(groupTourProductMPO != null){
            groupTourProductBody = new GroupTourBody();
            BeanUtils.copyProperties(groupTourProductMPO,groupTourProductBody);
            if(ListUtils.isNotEmpty(groupTourProductSetMealMPOS)){
                List<GroupTourProductSetMeal> meals = groupTourProductSetMealMPOS.stream().map(p->{
                    GroupTourProductSetMeal groupTourProductSetMeal = new GroupTourProductSetMeal();
                    BeanUtils.copyProperties(p,groupTourProductSetMeal);
                    //价格计算
                    IncreasePrice increasePrice = new IncreasePrice();
                    increasePrice.setChannelCode(groupTourProductMPO.getChannel());
                    increasePrice.setProductCode(groupTourProductMPO.getId());
                    increasePrice.setAppSource(request.getFrom());
                    increasePrice.setProductCategory(groupTourProductMPO.getCategory());
                    List<GroupTourPrice> groupTourPrices = p.getGroupTourPrices();
                    List<IncreasePriceCalendar> priceCalendars = groupTourPrices.stream().map(item -> {
                        IncreasePriceCalendar priceCalendar = new IncreasePriceCalendar();
                        priceCalendar.setAdtSellPrice(item.getAdtSellPrice());
                        priceCalendar.setChdSellPrice(item.getChdSellPrice());
                        priceCalendar.setDate(item.getDate());
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
            }
        }
        //加载推荐列表
        GroupTourListReq groupTourListReq = new GroupTourListReq();
        if(CollectionUtils.isNotEmpty(depCodes) && depCodes.size() > 1){
            groupTourListReq.setDepCityCode(request.getCityCode());
        }else{
            groupTourListReq.setDepPlace(request.getCityCode());
        }
        groupTourListReq.setArrCityCode(request.getArrCity());
        groupTourListReq.setGroupTourType(groupTourProductMPO.getGroupTourType());
        groupTourListReq.setApp(request.getFrom());
        List<ProductListMPO> productListMPOS = productDao.groupTourList(groupTourListReq);
        log.info("推荐列表：{}", JSONObject.toJSONString(productListMPOS));
        if (CollectionUtils.isNotEmpty(productListMPOS)) {
            List<GroupTourRecommend> groupTourRecommends = productListMPOS.stream().map(a -> {
                GroupTourRecommend groupTourRecommend = new GroupTourRecommend();
                groupTourRecommend.setCategory(a.getCategory());
                groupTourRecommend.setImage(a.getProductImageUrl());
                groupTourRecommend.setProductId(a.getProductId());
                groupTourRecommend.setProductName(a.getProductName());
                IncreasePrice increasePrice = productService.increasePrice(a, request.getFrom());
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
        List<ScenicSpotProductMPO> scenicSpotProductMPOS = scenicSpotDao.querySpotProduct(request.getScenicSpotId());
        String date = request.getDate();
        List<ScenicSpotProductBase> productBases = Lists.newArrayList();
        if(ListUtils.isNotEmpty(scenicSpotProductMPOS)){
            log.info("查询到的产品列表数据为：{}",JSON.toJSONString(scenicSpotProductMPOS));
            for (ScenicSpotProductMPO scenicSpotProduct : scenicSpotProductMPOS) {
                String productId = scenicSpotProduct.getId();
                //获取最近可定日期
                int bookBeforeDay = scenicSpotProduct.getScenicSpotProductTransaction().getBookBeforeDay();
                Date canBuyDate = getCanBuyDate(bookBeforeDay, scenicSpotProduct.getScenicSpotProductTransaction().getBookBeforeTime());
                List<ScenicSpotProductPriceMPO> scenicSpotProductPriceMPOS = scenicSpotDao.queryProductPriceByProductId(productId);
                // 根据产品id、规则id、票种 拆成多个产品
                Map<String, List<ScenicSpotProductPriceMPO>> priceMap = scenicSpotProductPriceMPOS.stream().collect(Collectors.groupingBy(price ->
                        String.format("%s-%s-%s", price.getScenicSpotProductId(), price.getScenicSpotRuleId(), price.getTicketKind())));
                log.info("产品{}拆成{}个", scenicSpotProduct.getId(), priceMap.size());
                priceMap.forEach((k, v) -> {
                    ScenicSpotProductPriceMPO scenicSpotProductPriceMPO = filterPrice(v, date, canBuyDate);
                    if(scenicSpotProductPriceMPO == null){
                        return;
                    }
                    String scenicSpotRuleId = scenicSpotProductPriceMPO.getScenicSpotRuleId();
                    ScenicSpotRuleMPO scenicSpotRuleMPO = scenicSpotDao.queryRuleById(scenicSpotRuleId);
                    if(scenicSpotRuleMPO == null){
                        log.info("产品因为价格规则无数据异常被过滤,当前产品id为{}, 当前适用价格信息为{}",scenicSpotProduct.getId(), JSON.toJSON(scenicSpotProductPriceMPO));
                        return;
                    }

                    ScenicSpotProductBase scenicSpotProductBase = new ScenicSpotProductBase();
                    String category = scenicSpotProduct.getScenicSpotProductBaseSetting().getCategoryCode();
                    BasePrice basePrice = new BasePrice();
                    BeanUtils.copyProperties(scenicSpotProductPriceMPO,basePrice);
                    //需要调用加价方法
                    IncreasePrice increasePrice = new IncreasePrice();
                    increasePrice.setChannelCode(scenicSpotProduct.getChannel());
                    increasePrice.setProductCode(scenicSpotProductPriceMPO.getScenicSpotProductId());
                    increasePrice.setAppSource(request.getFrom());
                    increasePrice.setProductCategory(category);
                    List<IncreasePriceCalendar> priceCalendars = new ArrayList<>(1);
                    IncreasePriceCalendar priceCalendar = new IncreasePriceCalendar();
                    priceCalendar.setAdtSellPrice(scenicSpotProductPriceMPO.getSellPrice());
                    priceCalendar.setDate(scenicSpotProductPriceMPO.getStartDate());
                    priceCalendars.add(priceCalendar);
                    increasePrice.setPrices(priceCalendars);
                    commonService.increasePrice(increasePrice);
                    List<IncreasePriceCalendar> prices = increasePrice.getPrices();
                    if(ListUtils.isEmpty(prices)){
                        log.info("产品因为价格计算之后无价格数据返回,当前产品id为{}, 当前适用价格信息为{}",scenicSpotProduct.getId(), JSON.toJSON(scenicSpotProductPriceMPO));
                        return;
                    }
                    BeanUtils.copyProperties(scenicSpotProductPriceMPO,basePrice,"sellPrice");
                    IncreasePriceCalendar increasePriceCalendar = prices.get(0);
                    basePrice.setSellPrice(increasePriceCalendar.getAdtSellPrice());
                    basePrice.setPriceId(scenicSpotProductPriceMPO.getId());
                    scenicSpotProductBase.setPrice(basePrice);

                    BeanUtils.copyProperties(scenicSpotProduct,scenicSpotProductBase);
                    productBases.add(scenicSpotProductBase);
                    scenicSpotProductBase.setCategory(category);
                    scenicSpotProductBase.setTicketKind(scenicSpotProductPriceMPO.getTicketKind());
                    scenicSpotProductBase.setProductId(scenicSpotProduct.getId());

                    //使用最近可定日期比较
                    //String startDate = scenicSpotProductPriceMPO.getStartDate();
                    String startDate = DateTimeUtil.formatDate(canBuyDate);
                    LocalDate localDate = LocalDate.now();
                    LocalDate tomorrow = localDate.plusDays(1);
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    String dateStr = localDate.format(fmt);
                    String tomorrowStr = tomorrow.format(fmt);
                    List<Tag> bookTag = new ArrayList<>(1);
                    Tag tag = null;
                    if(StringUtils.equals(startDate,dateStr)) {
                        tag = new Tag();
                        tag.setName("可订今日");
                        tag.setColour(ColourConstants.TICKET_BLUE);
                        bookTag.add(tag);
                    }else if(StringUtils.equals(startDate,tomorrowStr)){
                        tag = new Tag();
                        tag.setName("可订明日");
                        tag.setColour(ColourConstants.TICKET_BLUE);
                        bookTag.add(tag);
                    }
                    scenicSpotProductBase.setBookTag(bookTag);

                    List<Tag> ticketkind = new ArrayList<>();
                    String refundTag = scenicSpotRuleMPO.getRefundTag();
                    if(StringUtils.isNotEmpty(refundTag)){
                        Tag tag1 = new Tag();
                        if(StringUtils.equals("随心退",refundTag)){
                            tag1.setColour(ColourConstants.TICKET_GREEN);
                        }
                        tag1.setName(refundTag);
                        ticketkind.add(tag);
                    }
                    int ticketType = scenicSpotRuleMPO.getTicketType();
                    Tag tag1 = new Tag();
                    tag1.setName(0 == ticketType?"电子票":"实体票");
                    ticketkind.add(tag1);

                    int limitBuy = scenicSpotRuleMPO.getLimitBuy();
                    if(1 == limitBuy){
                        int maxCount = scenicSpotRuleMPO.getMaxCount();
                        Tag tag2 = new Tag();
                        tag2.setName("限购".concat(String.valueOf(maxCount)).concat("张/单"));
                        tag2.setColour(ColourConstants.TICKET_BLUE);
                        ticketkind.add(tag2);
                    }
                    ScenicSpotProductTransaction scenicSpotProductTransaction = scenicSpotProduct.getScenicSpotProductTransaction();
                    if(scenicSpotProductTransaction != null){
                        int ticketOutHour = scenicSpotProductTransaction.getTicketOutHour();
                        int ticketOutMinute = scenicSpotProductTransaction.getTicketOutMinute();
                        if( 0== ticketOutHour &&0 == ticketOutMinute){
                            Tag tag2 = new Tag();
                            tag2.setName("随订随用");
                            ticketkind.add(tag2);
                            scenicSpotProductBase.setAnyTime(1);
                        }
                    }
                    scenicSpotProductBase.setTicketkindTag(ticketkind);
                });

            }
        }
        return BaseResponse.withSuccess(productBases);
    }

    private Date getCanBuyDate(int bookBeforeDay, String bookBeforeTime) {
        Date current = new Date();
        if (StringUtils.isNotBlank(bookBeforeTime)) {
            if (StringUtils.equals(bookBeforeTime, "00:00")) {
                bookBeforeDay+=1;
            }else{
                if(bookBeforeDay == 0){
                    Date bookDateTime = DateTimeUtil.parse(DateTimeUtil.formatDate(current, DateTimeUtil.YYYYMMDD)+" " + bookBeforeTime, DateTimeUtil.YYYYMMDDHHmm);
                    if(bookDateTime.compareTo(current) < 0){
                        bookBeforeDay += 1;
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
                //2021-08-05 重新处理过滤价格，需要兼容普通日历类型库存
                Date reqDate = DateTimeUtil.parseDate(date);
                if(reqDate.compareTo(canBuyDate) < 0){
                    //所选日期小于最近可定日期，直接忽略
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
        String scenicSpotId = request.getScenicSpotId();
        String startDate = request.getStartDate();
        String productId = request.getProductId();
        String endDate = request.getEndDate();
        String packageId = request.getPackageId();
        List<BasePrice> basePrices = null;
        List<ScenicSpotProductPriceMPO> effective = new ArrayList<>();
        if (StringUtils.isEmpty(productId)) {
            List<ScenicSpotProductMPO> scenicSpotProductMPOS = scenicSpotDao.querySpotProduct(scenicSpotId);
            if (ListUtils.isNotEmpty(scenicSpotProductMPOS)) {
                for (ScenicSpotProductMPO productMPO : scenicSpotProductMPOS) {
                    String productMPOId = productMPO.getId();
                    //获取最近可定日期
                    int bookBeforeDay = productMPO.getScenicSpotProductTransaction().getBookBeforeDay();
                    Date canBuyDate = getCanBuyDate(bookBeforeDay, productMPO.getScenicSpotProductTransaction().getBookBeforeTime());
                    int sellType = productMPO.getSellType();
                    //普通库存是一段时间 需要拆分
                    if (sellType == 0) {
                        List<ScenicSpotProductPriceMPO> scenicSpotProductPriceMPOS = scenicSpotDao.queryPriceByProductIdAndDate(productMPOId, null, null);
                        for (ScenicSpotProductPriceMPO scenicSpotProductPriceMPO : scenicSpotProductPriceMPOS) {
                            if (StringUtils.isNotBlank(request.getPackageId()) && !scenicSpotProductPriceMPO.getId().equals(request.getPackageId())){
                                continue;
                            }
                            List<ScenicSpotProductPriceMPO> scenicSpotProductPriceMPOS1 = splitCalendar(scenicSpotProductPriceMPO, startDate, endDate, canBuyDate);
                            if (ListUtils.isNotEmpty(scenicSpotProductMPOS)) {
                                effective.addAll(scenicSpotProductPriceMPOS1);
                            }
                        }

                    }else{
                        List<ScenicSpotProductPriceMPO> priceMPOS = scenicSpotDao.queryPriceByProductIdAndDate(productMPOId, startDate, endDate);
                        for (ScenicSpotProductPriceMPO scenicSpotProductPriceMPO : priceMPOS){
                            if (StringUtils.isNotBlank(request.getPackageId()) && !scenicSpotProductPriceMPO.getId().equals(request.getPackageId())){
                                continue;
                            }
                            Date saleDate = DateTimeUtil.parseDate(scenicSpotProductPriceMPO.getStartDate());
                            if(canBuyDate.compareTo(saleDate) >= 0){
                                continue;
                            }
                            effective.add(scenicSpotProductPriceMPO);
                        }
                    }

                }
            }

        }else {
            List<ScenicSpotProductPriceMPO> scenicSpotProductPriceMPOS = getPrice(productId, packageId, startDate, endDate);
            ScenicSpotProductMPO scenicSpotProductMPO = scenicSpotDao.querySpotProductById(productId);
            //获取最近可定日期
            int bookBeforeDay = scenicSpotProductMPO.getScenicSpotProductTransaction().getBookBeforeDay();
            Date canBuyDate = getCanBuyDate(bookBeforeDay, scenicSpotProductMPO.getScenicSpotProductTransaction().getBookBeforeTime());
            if (ListUtils.isNotEmpty(scenicSpotProductPriceMPOS)) {
                log.info("通过产品id查询到的价格信息{}", JSON.toJSONString(scenicSpotProductPriceMPOS));
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
                        if(canBuyDate.compareTo(saleDate) >= 0){
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
            //每日价格取最小值
            for (String date : dates) {
                List<ScenicSpotProductPriceMPO> priceMPOS = priceMapByDate.get(date);
                priceMPOS.sort(Comparator.comparing(a -> a.getSellPrice()));
                finalPriceList.add(priceMPOS.get(0));
            }
            effective = finalPriceList.stream().sorted(Comparator.comparing(ScenicSpotProductPriceMPO::getStartDate)).collect(Collectors.toList());
            basePrices = effective.stream().map(p -> {
                BasePrice basePrice = new BasePrice();
                BeanUtils.copyProperties(p, basePrice);
                //需要调用加价方法
                IncreasePrice increasePrice = new IncreasePrice();
                ScenicSpotProductMPO scenicSpotProductMPO = scenicSpotDao.querySpotProductById(p.getScenicSpotProductId());
                if(scenicSpotProductMPO != null){
                    increasePrice.setChannelCode(scenicSpotProductMPO.getChannel());
                }
                //increasePrice.setChannelCode(request.getChannelCode());
                increasePrice.setProductCode(p.getScenicSpotProductId());
                increasePrice.setAppSource(request.getFrom());
                increasePrice.setProductCategory("d_ss_ticket");
                List<IncreasePriceCalendar> priceCalendars = new ArrayList<>(1);
                IncreasePriceCalendar priceCalendar = new IncreasePriceCalendar();
                priceCalendar.setAdtSellPrice(p.getSellPrice());
                priceCalendar.setDate(p.getStartDate());
                priceCalendars.add(priceCalendar);
                increasePrice.setPrices(priceCalendars);
                commonService.increasePrice(increasePrice);
                List<IncreasePriceCalendar> prices = increasePrice.getPrices();
                IncreasePriceCalendar priceCalendar1 = prices.get(0);
                basePrice.setSellPrice(priceCalendar1.getAdtSellPrice());
                basePrice.setPriceId(p.getId());
                return basePrice;
            }).collect(Collectors.toList());
        }
        return BaseResponse.withSuccess(basePrices);
    }

    private List<ScenicSpotProductPriceMPO> getPrice(String productId, String packageId, String startDate, String endDate){
        String ruleId = null;
        String ticketKind = null;
        // 价格日历应该查询和packageId属于同一套餐的价格，只有productid不准确
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
     * 根据日期 找到对应日期的 星期
     */
     private static String getDayOfWeekByDate(String date) {
        String dayOfweek = "0";
        String [] weekNum ={"星期一","星期二","星期三","星期四","星期五","星期六","星期日"};
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
             Date dBegin = simpleDateFormat.parse(sdate);// 定义起始日期
             Date dEnd = simpleDateFormat.parse(edate);// 定义结束日期

             List<ScenicSpotProductPriceMPO> lDate = new ArrayList();
             ScenicSpotProductPriceMPO st = new ScenicSpotProductPriceMPO();
             BeanUtils.copyProperties(scenicSpotProductPriceMPO,st,"startDate","endDate");
             st.setStartDate(startDate);
             st.setEndDate(startDate);
             lDate.add(st);

             Calendar calBegin = Calendar.getInstance();
             // 使用给定的 Date 设置此 Calendar 的时间
             calBegin.setTime(dBegin);
             Calendar calEnd = Calendar.getInstance();
             // 使用给定的 Date 设置此 Calendar 的时间
             calEnd.setTime(dEnd);
             // 测试此日期是否在指定日期之后
             while (dEnd.compareTo(calBegin.getTime())>= 0) {
                 // 根据日历的规则，为给定的日历字段添加或减去指定的时间量
                 log.info("canBuyDate:{}, calBegin:{}", DateTimeUtil.formatDate(canBuyDate), DateTimeUtil.formatDate(calBegin.getTime()));
                 if(canBuyDate.compareTo(calBegin.getTime()) > 0){
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
      * 跟团游价格日历
      * [request]
      * @return {@link BaseResponse< ProductPriceCalendarResult>}
      * @throws
      */
    @Override
    public BaseResponse<ProductPriceCalendarResult> queryGroupTourPriceCalendar(CalendarRequest request) {

        ProductPriceCalendarResult result = new ProductPriceCalendarResult();

        GroupTourProductMPO groupTourProductMPO = groupTourDao.queryTourProduct(request.getProductId());
        GroupTourProductSetMealMPO groupTourProductSetMealMPO = groupTourDao.queryGroupSetMealBySetId(request.getSetMealId());
        List<GroupTourPrice> groupTourPrices = groupTourProductSetMealMPO.getGroupTourPrices();
        int beforeBookDay = groupTourProductMPO.getGroupTourProductPayInfo().getBeforeBookDay();
        //过滤日期
        Date startDate = new Date();
        if(StringUtils.isNotBlank(request.getStartDate())){
            Date reqStartDate = DateTimeUtil.parse(request.getStartDate(), DateTimeUtil.YYYYMMDD);
            if(DateTimeUtil.getDateDiffDays(reqStartDate, startDate) > 0) {
                startDate = reqStartDate;
            }
        }
        //根据提前预定天数处理搜索的开始时间
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
            //价格计算
            IncreasePrice increasePrice = new IncreasePrice();
            increasePrice.setChannelCode(groupTourProductMPO.getChannel());
            increasePrice.setProductCode(groupTourProductMPO.getId());
            increasePrice.setAppSource(request.getFrom());
            increasePrice.setProductCategory(groupTourProductMPO.getCategory());
            List<IncreasePriceCalendar> priceCalendars = groupTourPrices.stream().map(item -> {
               IncreasePriceCalendar priceCalendar = new IncreasePriceCalendar();
               priceCalendar.setAdtSellPrice(item.getAdtSellPrice());
               priceCalendar.setChdSellPrice(item.getChdSellPrice());
               priceCalendar.setDate(item.getDate());
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
        ScenicSpotProductDetail scenucSpotProductDetail =null;
        ScenicSpotProductMPO scenicSpotProductMPO = scenicSpotDao.querySpotProductById(request.getProductId());
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
     * 酒景套餐产品详情 产品简要信息，可直接查询返回
     * [request]
     * @return {@link BaseResponse< HotelScenicProductDetail>}
     * @throws
     */
    @Override
    public BaseResponse<HotelScenicProductDetail> hotelScenicProductDetail(HotelScenicProductRequest request) {
        HotelScenicSpotProductMPO hotelScenicSpotProductMPO = hotelScenicDao.queryHotelScenicProductMpoById(request.getProductId());

        HotelScenicProductDetail hotelScenicProductDetail = new HotelScenicProductDetail();
        BeanUtils.copyProperties(hotelScenicSpotProductMPO, hotelScenicProductDetail);
        hotelScenicProductDetail.setProductId(hotelScenicSpotProductMPO.getId());
        hotelScenicProductDetail.setDesc(hotelScenicSpotProductMPO.getComputerDesc());
        return BaseResponse.withSuccess(hotelScenicProductDetail);
    }

    /**
     * @author: wangying
     * @date 5/26/21 10:01 AM
     * 套餐列表
     * [request]
     * @return {@link BaseResponse< List< HotelScenicProductSetMealBrief>>}
     * @throws
     */
    @Override
    public BaseResponse<List<HotelScenicProductSetMealBrief>> hotelScenicProductSetMealList(CalendarRequest request) {
        List<HotelScenicSpotProductSetMealMPO> setMealMpoList = hotelScenicDao.queryHotelScenicSetMealList(request);
        HotelScenicSpotProductMPO productMPO = hotelScenicDao.queryHotelScenicProductMpoById(request.getProductId());
        List<HotelScenicProductSetMealBrief> briefList = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(setMealMpoList)){
            briefList = setMealMpoList.stream().map(setMealMpo -> {
                HotelScenicProductSetMealBrief brief = new HotelScenicProductSetMealBrief();
                BeanUtils.copyProperties(setMealMpo, brief);
                brief.setPackageId(setMealMpo.getId());
                brief.setProductId(setMealMpo.getHotelScenicSpotProductId());
                brief.setTitle(setMealMpo.getName());
                brief.setThemeElements(productMPO.getPayInfo() == null ? 0 : productMPO.getPayInfo().getThemeElements());
                //价格计算
                IncreasePrice increasePrice = hotelIncreasePrice(productMPO, request, setMealMpo.getPriceStocks());
                log.info("价格日历为："+ JSON.toJSONString(increasePrice));
                brief.setPrice(increasePrice.getPrices().stream().filter(a -> StringUtils.isBlank(request.getStartDate()) ? true : StringUtils.equals(a.getDate(), request.getStartDate())).collect(Collectors.toList()).get(0).getAdtSellPrice());
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
        increasePrice.setProductCategory(productMPO.getCategory());
        List<IncreasePriceCalendar> priceCalendars = priceStocks.stream().map(item -> {
            IncreasePriceCalendar priceCalendar = new IncreasePriceCalendar();
            priceCalendar.setAdtSellPrice(item.getAdtPrice());
            priceCalendar.setChdSellPrice(item.getChdPrice());
            priceCalendar.setDate(item.getDate());
            return priceCalendar;
        }).collect(Collectors.toList());
        increasePrice.setPrices(priceCalendars);
        commonService.increasePrice(increasePrice);
        return increasePrice;
    }

    /**
     * @author: wangying
     * @date 5/26/21 2:18 PM
     * 酒景套餐价格日历
     * [request]
     * @return {@link BaseResponse< ProductPriceCalendarResult>}
     * @throws
     */
    @Override
    public BaseResponse<ProductPriceCalendarResult> queryHotelScenicPriceCalendar(CalendarRequest request) {
        ProductPriceCalendarResult result = new ProductPriceCalendarResult();
        List<HotelScenicSpotProductSetMealMPO> setMealMpoList = hotelScenicDao.queryHotelScenicSetMealList(request);
        HotelScenicSpotProductMPO productMPO = hotelScenicDao.queryHotelScenicProductMpoById(request.getProductId());
        List<HotelScenicSpotPriceStock> allPriceStocks = new ArrayList<>();
        //将所有价格整合到一起
        for (HotelScenicSpotProductSetMealMPO hotelScenicSpotProductSetMealMPO : setMealMpoList) {
            allPriceStocks.addAll(hotelScenicSpotProductSetMealMPO.getPriceStocks());
        }
        HotelScenicSpotProductPayInfo payInfo = productMPO.getPayInfo();
        if(payInfo != null && payInfo.getSellType() == 0){
            //普通库存拆分价格日历
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
            //价格数据为空，无价格
            return BaseResponse.withSuccess(result);
        }
        //价格根据日期分组
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
        //筛选最低价格并过滤非查询日期
        for (String date : dates) {
            Date calendarDate = DateTimeUtil.parse(date, DateTimeUtil.YYYYMMDD);
            if(DateTimeUtil.getDateDiffDays(calendarDate, startDate) < 0 || DateTimeUtil.getDateDiffDays(calendarDate, endDate) > 0){
                continue;
            }
            List<HotelScenicSpotPriceStock> tmpPriceStock = priceMapByDate.get(date);
            tmpPriceStock.sort(Comparator.comparing(a -> a.getAdtPrice()));
            priceStocks.add(tmpPriceStock.get(0));
        }
        Map<String, HotelScenicSpotPriceStock> priceStocksByDate = Maps.uniqueIndex(priceStocks, a -> a.getDate());
        //加价
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
                            //在不支持售卖的日期区间内
                            canSell = false;
                            break;
                        }
                    }
                }
                if (canSell && StringUtils.isNotEmpty(payInfo.getExUseWeek())) {
                    //处理过滤不可售卖星期
                    String dayOfWeekByDate = getDayOfWeekByDate(date);
                    if (StringUtils.contains(payInfo.getExUseWeek(), dayOfWeekByDate)) {
                        //属于不可售卖星期
                        canSell = false;
                    }
                }
                if(canSell){
                    //可以售卖的日期
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
     * 酒景套餐详情
     * [request]
     * @return {@link BaseResponse< HotelScenicProductSetMealDetail>}
     * @throws
     */
    @Override
    public BaseResponse<HotelScenicProductSetMealDetail> queryHotelScenicSetMealDetail(HotelScenicSetMealRequest request) {
        HotelScenicSpotProductSetMealMPO setMealMPO = hotelScenicDao.queryHotelScenicSetMealById(request);
        HotelScenicSpotProductMPO productMPO = hotelScenicDao.queryHotelScenicProductMpoById(request.getProductId());
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
     * 酒店详情
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

}
