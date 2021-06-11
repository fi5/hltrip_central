package com.huoli.trip.central.web.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.huoli.trip.central.api.ProductV2Service;
import com.huoli.trip.central.web.constant.ColourConstants;
import com.huoli.trip.central.web.dao.GroupTourDao;
import com.huoli.trip.central.web.dao.ScenicSpotDao;
import com.huoli.trip.central.web.service.CommonService;
import com.huoli.trip.common.entity.mpo.groupTour.GroupTourPrice;
import com.huoli.trip.common.entity.mpo.groupTour.GroupTourProductMPO;
import com.huoli.trip.common.entity.mpo.groupTour.GroupTourProductSetMealMPO;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.*;
import com.huoli.trip.common.util.DateTimeUtil;
import com.huoli.trip.common.util.ListUtils;
import com.huoli.trip.common.vo.IncreasePrice;
import com.huoli.trip.common.vo.IncreasePriceCalendar;
import com.huoli.trip.common.vo.PriceInfo;
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
    private CommonService commonService;

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
        final List<GroupTourProductSetMealMPO> groupTourProductSetMealMPOS = groupTourDao.queryProductSetMealByProductId(groupTourProductMPO.getId(), depCodes);
        log.info("查询到的套餐列表：{}", JSONObject.toJSONString(groupTourProductSetMealMPOS));
        if(groupTourProductMPO != null){
            groupTourProductBody = new GroupTourBody();
            BeanUtils.copyProperties(groupTourProductMPO,groupTourProductBody);
            if(ListUtils.isNotEmpty(groupTourProductSetMealMPOS)){
                List<GroupTourProductSetMeal> meals = groupTourProductSetMealMPOS.stream().map(p->{
                    GroupTourProductSetMeal groupTourProductSetMeal = new GroupTourProductSetMeal();
                    BeanUtils.copyProperties(p,groupTourProductSetMeal);
                    return groupTourProductSetMeal;
                }).collect(Collectors.toList());
                groupTourProductBody.setSetMeals(meals);
            }
        }
        return BaseResponse.withSuccess(groupTourProductBody);
    }

    @Override
    public GroupMealsBody groupMealsBody(GroupTourMealsRequest request) {
        GroupMealsBody body = null;
        List<GroupTourProductSetMealMPO> groupTourProductSetMealMPOS = groupTourDao.queryProductSetMealByProductId(request.getGroupTourId(), null);
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
        List<ScenicSpotProductBase> productBases = null;
        if(ListUtils.isNotEmpty(scenicSpotProductMPOS)){
            productBases = new ArrayList<>();
            for (ScenicSpotProductMPO scenicSpotProduct : scenicSpotProductMPOS) {
                String productId = scenicSpotProduct.getId();
                List<ScenicSpotProductPriceMPO> scenicSpotProductPriceMPOS = scenicSpotDao.queryProductPriceByProductId(productId);
                ScenicSpotProductPriceMPO scenicSpotProductPriceMPO = filterPrice(scenicSpotProductPriceMPOS, date);
                if(scenicSpotProductPriceMPO == null){
                    continue;
                }
                String scenicSpotRuleId = scenicSpotProductPriceMPO.getScenicSpotRuleId();
                ScenicSpotRuleMPO scenicSpotRuleMPO = scenicSpotDao.queryRuleById(scenicSpotRuleId);
                if(scenicSpotRuleMPO == null){
                    log.info("产品因为价格规则无数据异常被过滤,当前产品id为{}, 当前适用价格信息为{}",scenicSpotProduct.getId(), JSON.toJSON(scenicSpotProductPriceMPO));
                    continue;
                }
                ScenicSpotProductBase scenicSpotProductBase = new ScenicSpotProductBase();
                BeanUtils.copyProperties(scenicSpotProduct,scenicSpotProductBase);
                productBases.add(scenicSpotProductBase);
                String category = scenicSpotProduct.getScenicSpotProductBaseSetting().getCategoryCode();
                scenicSpotProductBase.setCategory(category);
                scenicSpotProductBase.setTicketKind(scenicSpotProductPriceMPO.getTicketKind());

                String startDate = scenicSpotProductPriceMPO.getStartDate();
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
                        tag2.setName("随买随用");
                        ticketkind.add(tag2);
                    }
                }

                scenicSpotProductBase.setTicketkindTag(ticketkind);
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
                commonService.increasePrice(increasePrice);
                List<IncreasePriceCalendar> prices = increasePrice.getPrices();
                if(ListUtils.isEmpty(prices)){
                    log.info("产品因为价格计算之后无价格数据返回,当前产品id为{}, 当前适用价格信息为{}",scenicSpotProduct.getId(), JSON.toJSON(scenicSpotProductPriceMPO));
                    continue;
                }
                BeanUtils.copyProperties(scenicSpotProductPriceMPO,basePrice,"sellPrice");
                IncreasePriceCalendar increasePriceCalendar = prices.get(0);
                basePrice.setSellPrice(increasePriceCalendar.getAdtSellPrice());
                basePrice.setPriceId(scenicSpotProductPriceMPO.getId());
                scenicSpotProductBase.setPrice(basePrice);
            }
        }
        return BaseResponse.withSuccess(productBases);
    }

    private ScenicSpotProductPriceMPO filterPrice(List<ScenicSpotProductPriceMPO> list,String date){
        if(ListUtils.isEmpty(list)){
            return null;
        }else{
            if(StringUtils.isNotEmpty(date)){
                List<ScenicSpotProductPriceMPO> collect = list.stream().filter(p -> StringUtils.equals(p.getStartDate(), date)).collect(Collectors.toList());
                if(ListUtils.isEmpty(collect)){
                    return null;
                }else{
                    String dayOfWeekByDate = getDayOfWeekByDate(date);
                    if(StringUtils.equals("0",dayOfWeekByDate)){
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
        List<BasePrice> basePrices = null;
        List<ScenicSpotProductPriceMPO> effective = new ArrayList<>();
        if(StringUtils.isEmpty(productId)){
            List<ScenicSpotProductMPO> scenicSpotProductMPOS = scenicSpotDao.querySpotProduct(scenicSpotId);
            if(ListUtils.isNotEmpty(scenicSpotProductMPOS)){
                for(ScenicSpotProductMPO productMPO : scenicSpotProductMPOS){
                    String productMPOId = productMPO.getId();
                    int sellType = productMPO.getSellType();
                    //普通库存是一段时间 需要拆分
                    if(sellType == 0){
                        List<ScenicSpotProductPriceMPO> scenicSpotProductPriceMPOS = scenicSpotDao.queryPriceByProductIdAndDate(productMPOId,null,null);
                        for(ScenicSpotProductPriceMPO scenicSpotProductPriceMPO : scenicSpotProductPriceMPOS) {
                            List<ScenicSpotProductPriceMPO> scenicSpotProductPriceMPOS1 = splitCalendar(scenicSpotProductPriceMPO, startDate, endDate);
                            if(ListUtils.isNotEmpty(scenicSpotProductMPOS)){
                                effective.addAll(scenicSpotProductPriceMPOS1);
                            }
                        }

                    }else{
                        effective = scenicSpotDao.queryPriceByProductIdAndDate(productMPOId,startDate,endDate);
                    }

                }
            }

        }else {
            List<ScenicSpotProductPriceMPO> scenicSpotProductPriceMPOS = scenicSpotDao.queryProductPriceByProductId(productId);
            if (ListUtils.isNotEmpty(scenicSpotProductPriceMPOS)) {
                for (ScenicSpotProductPriceMPO s : scenicSpotProductPriceMPOS) {
                    String startDate1 = s.getStartDate();
                    String endDate1 = s.getEndDate();
                    if (!StringUtils.equals(startDate1, endDate1)) {
                        List<ScenicSpotProductPriceMPO> scenicSpotProductPriceMPOS1 = splitCalendar(s, startDate, endDate);
                        if (ListUtils.isNotEmpty(scenicSpotProductPriceMPOS1)) {
                            effective.addAll(scenicSpotProductPriceMPOS1);
                        }
                    } else {
                        effective.add(s);
                    }
                }
            }
        }

        if(ListUtils.isNotEmpty(effective)){
            List<ScenicSpotProductPriceMPO>  fe = new ArrayList<>(effective.size());
            for (ScenicSpotProductPriceMPO  ss: effective) {
                String startDate1 = ss.getStartDate();
                String dayOfWeekByDate = getDayOfWeekByDate(startDate1);
                if(ss.getWeekDay().contains(dayOfWeekByDate)){
                    fe.add(ss);
                }

            }
            effective = fe.stream().sorted(Comparator.comparing(ScenicSpotProductPriceMPO :: getStartDate)).collect(Collectors.toList());
            basePrices = effective.stream().map(p->{
                BasePrice basePrice = new BasePrice();
                BeanUtils.copyProperties(p,basePrice);
                //需要调用加价方法
                IncreasePrice increasePrice = new IncreasePrice();
                increasePrice.setChannelCode(request.getChannelCode());
                increasePrice.setProductCode(p.getScenicSpotProductId());
                increasePrice.setAppSource(request.getFrom());
                increasePrice.setProductCategory("d_ss_ticket");
                List<IncreasePriceCalendar> priceCalendars = new ArrayList<>(1);
                IncreasePriceCalendar priceCalendar = new IncreasePriceCalendar();
                priceCalendar.setAdtSellPrice(p.getSellPrice());
                priceCalendar.setDate(p.getStartDate());
                priceCalendars.add(priceCalendar);
                commonService.increasePrice(increasePrice);
                List<IncreasePriceCalendar> prices = increasePrice.getPrices();
                IncreasePriceCalendar priceCalendar1 = prices.get(0);
                basePrice.setSellPrice(priceCalendar1.getAdtSellPrice());
                return basePrice;
            }).collect(Collectors.toList());
        }
        return BaseResponse.withSuccess(basePrices);
    }

    /**
     * 根据日期 找到对应日期的 星期
     */
     private static String getDayOfWeekByDate(String date) {
        String dayOfweek = "0";
        String [] weekNum ={"星期一","星期二 ","星期三","星期四","星期五","星期六","星期日"};
         List<String> list = Arrays.asList(weekNum);
         try {
            SimpleDateFormat myFormatter = new SimpleDateFormat("yyyy-MM-dd");
            Date myDate = myFormatter.parse(date);
            SimpleDateFormat formatter = new SimpleDateFormat("E");
            String str = formatter.format(myDate);
            if(list.contains(str)){
                dayOfweek = String.valueOf(list.indexOf(str)+1);
            }
            } catch (Exception e) {

            }
            return dayOfweek;
     }

     private List<ScenicSpotProductPriceMPO> splitCalendar(ScenicSpotProductPriceMPO scenicSpotProductPriceMPO,String startDate,String endDate) {
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
             while (dEnd.after(calBegin.getTime())) {
                 // 根据日历的规则，为给定的日历字段添加或减去指定的时间量
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

        GroupTourProductSetMealMPO groupTourProductSetMealMPO = groupTourDao.queryGroupSetMealBySetId(request.getSetMealId());
        List<GroupTourPrice> groupTourPrices = groupTourProductSetMealMPO.getGroupTourPrices();
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
        GroupTourProductMPO groupTourProductMPO = groupTourDao.queryTourProduct(request.getProductId());
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

}