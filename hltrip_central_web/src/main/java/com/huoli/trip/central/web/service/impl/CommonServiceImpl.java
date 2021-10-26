package com.huoli.trip.central.web.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.huoli.flight.server.api.CouponDeliveryService;
import com.huoli.flight.server.api.vo.flight.CouponSendParam;
import com.huoli.flight.server.api.vo.flight.CouponSuccess;
import com.huoli.trip.central.web.dao.GroupTourProductSetMealDao;
import com.huoli.trip.central.web.dao.HotelScenicSpotProductSetMealDao;
import com.huoli.trip.central.web.dao.ScenicSpotProductPriceDao;
import com.huoli.trip.central.web.dao.SupplierPolicyDao;
import com.huoli.trip.central.web.service.CommonService;
import com.huoli.trip.common.constant.ProductCategoryEnum;
import com.huoli.trip.common.entity.SupplierPolicyPO;
import com.huoli.trip.common.entity.mpo.groupTour.GroupTourPrice;
import com.huoli.trip.common.entity.mpo.groupTour.GroupTourProductSetMealMPO;
import com.huoli.trip.common.entity.mpo.hotelScenicSpot.HotelScenicSpotPriceStock;
import com.huoli.trip.common.entity.mpo.hotelScenicSpot.HotelScenicSpotProductSetMealMPO;
import com.huoli.trip.common.entity.mpo.scenicSpotTicket.ScenicSpotProductPriceMPO;
import com.huoli.trip.common.util.BigDecimalUtil;
import com.huoli.trip.common.util.ListUtils;
import com.huoli.trip.common.vo.IncreasePrice;
import com.huoli.trip.common.vo.IncreasePriceCalendar;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2021/4/27<br>
 */
@Service
@Slf4j
public class CommonServiceImpl implements CommonService {

    @Autowired
    private SupplierPolicyDao supplierPolicyDao;

    @Autowired
    private ScenicSpotProductPriceDao scenicSpotProductPriceDao;

    @Autowired
    private GroupTourProductSetMealDao groupTourProductSetMealDao;

    @Autowired
    private HotelScenicSpotProductSetMealDao hotelScenicSpotProductSetMealDao;

    @Reference(group = "${flight_dubbo_group}", timeout = 60000, check = false, retries = 3)
    CouponDeliveryService couponDeliveryService;

    public void increasePrice_(IncreasePrice increasePrice){
        try {
            if(increasePrice == null){
                log.error("加价失败，参数为空。");
                return;
            }
            log.info("准备获取加价配置。。原始条件和价格={}", JSON.toJSONString(increasePrice));
            if(ListUtils.isEmpty(increasePrice.getPrices())){
                log.error("加价失败，价格列表为空。");
                return;
            }
            List<SupplierPolicyPO> supplierPolices = supplierPolicyDao.getSupplierPolicy(increasePrice);
            if(ListUtils.isEmpty(supplierPolices)){
                log.info("没有查到相关加价配置");
                return;
            }
            log.info("查到的加价配置={}", JSON.toJSONString(supplierPolices));
//            SupplierPolicyPO supplierPolicy = supplierPolices.stream().sorted(Comparator.comparing(sp -> {
//                String formula = sp.getPriceFormula().replace(" ", "");
//                Pattern pattern = Pattern.compile("(?<=price\\*\\(1\\+)\\d(\\.\\d{1,4})*(?=\\))");
//                Matcher matcher = pattern.matcher(formula);
//                if (matcher.find()){
//                    try {
//                        return Double.valueOf(matcher.group());
//                    } catch (Exception e) {
//                        log.error("加价公式有错误，加价配置={}", JSON.toJSONString(sp), e);
//                        return Double.MAX_VALUE;
//                    }
//                }
//                return Double.MAX_VALUE;
//            }, Double::compareTo)).findFirst().get();
            ScriptEngine se = new ScriptEngineManager().getEngineByName("JavaScript");
            IncreasePriceCalendar priceSample = increasePrice.getPrices().stream().filter(p ->
                    p.getAdtSellPrice() != null && p.getAdtSellPrice().compareTo(new BigDecimal(0)) == 1).findFirst().orElse(null);
            SupplierPolicyPO supplierPolicy = supplierPolices.stream().filter(sp -> {
                try {
                    se.eval(sp.getPriceFormula().replace("price",
                            priceSample.getAdtSellPrice().toPlainString()));
                    return true;
                } catch (Exception e) {
                    log.error("加价公式错误，id={}, formula={}", sp.getId(), sp.getPriceFormula(), e);
                    return false;
                }
            }).sorted(Comparator.comparing(sp -> {
                try {
                    return (Double) se.eval(sp.getPriceFormula().replace("price",
                            priceSample.getAdtSellPrice().toPlainString()));
                } catch (Exception e) {
                    log.error("加价公式错误，id={}, formula={}", sp.getId(), sp.getPriceFormula(), e);
                    return Double.MAX_VALUE;
                }
            }, Double::compareTo)).findFirst().get();
            log.info("找到合适的加价配置，使用这条配置加价，配置={}", JSON.toJSONString(supplierPolicy));
            for (IncreasePriceCalendar price : increasePrice.getPrices()) {
                price.setTag(supplierPolicy.getTag());
                price.setTagDesc(supplierPolicy.getTagDesc());
                // 加价计算
                if(price.getAdtSellPrice() != null){
                    BigDecimal newPrice = BigDecimal.valueOf((Double) se.eval(supplierPolicy.getPriceFormula().replace("price",
                            price.getAdtSellPrice().toPlainString()))).setScale(0, BigDecimal.ROUND_HALF_UP);
                    price.setAdtSellPrice(newPrice);
                }
                // 如果有儿童价也加价
                if(price.getChdSellPrice() != null){
                    String formula = supplierPolicy.getPriceFormula();
                    // 如果儿童单独配置了加价规则就用儿童的
                    if(StringUtils.isNotBlank(supplierPolicy.getChdPriceFormula())){
                        formula = supplierPolicy.getChdPriceFormula();
                    }
                    BigDecimal newPrice = BigDecimal.valueOf((Double) se.eval(formula.replace("price",
                            price.getChdSellPrice().toPlainString()))).setScale(0, BigDecimal.ROUND_HALF_UP);;
                    price.setChdSellPrice(newPrice);
                }
            }
            log.info("加价完成，加价后价格={}", JSON.toJSONString(increasePrice.getPrices()));
        } catch (Exception e) {
            log.error("加价计算失败，不影响主流程，channel = {}, productCode = {}", increasePrice.getChannelCode(), increasePrice.getProductCode(), e);
        }
    }

    @Override
    public void increasePrice(IncreasePrice increasePrice){
        try {
            if(increasePrice == null){
                log.error("加价失败，参数为空。");
                return;
            }
            log.info("准备获取加价配置。。原始条件和价格={}", JSON.toJSONString(increasePrice));
            if(ListUtils.isEmpty(increasePrice.getPrices())){
                log.error("加价失败，价格列表为空。");
                return;
            }
            SupplierPolicyPO supplierPolicy = getPolicy(increasePrice);
            ScriptEngine se = new ScriptEngineManager().getEngineByName("JavaScript");
            for (IncreasePriceCalendar price : increasePrice.getPrices()) {
                // 成人
                if(price.getAdtSettlePrice() != null && price.getAdtSettlePrice().compareTo(new BigDecimal(0)) == 1){
                    if(price.isAdtFloatPriceManually()){
                        // 人工维护加价计算
                        double floatPrice = formatBigDecimal(price.getAdtFloatPrice()).doubleValue();
                        double settlePrice = formatBigDecimal(price.getAdtSettlePrice()).doubleValue();
                        if(price.getAdtFloatPriceType() != null && price.getAdtFloatPriceType() == 1){
                            price.setAdtSellPrice(formatBigDecimalInt(BigDecimal.valueOf(BigDecimalUtil.add(settlePrice, floatPrice))));
                        } else if(price.getAdtFloatPriceType() != null && price.getAdtFloatPriceType() == 2){
                            price.setAdtSellPrice(formatBigDecimalInt(BigDecimal.valueOf(BigDecimalUtil.add(settlePrice, BigDecimalUtil.mul(settlePrice, floatPrice == 0 ? 1 : floatPrice)))));
                        }
                    } else if(supplierPolicy != null){
                        // 政策加价计算
                        BigDecimal newPrice = formatBigDecimalInt(BigDecimal.valueOf((Double) se.eval(supplierPolicy.getPriceFormula().replace("price",
                                price.getAdtSellPrice().toPlainString()))));
                        price.setAdtSellPrice(newPrice);
                    }
                }

                if(price.getChdSettlePrice() != null && price.getChdSettlePrice().compareTo(new BigDecimal(0)) == 1) {
                    // 儿童
                    if (price.isChdFloatPriceManually()) {
                        // 人工维护加价计算
                        double floatPrice = formatBigDecimal(price.getChdFloatPrice()).doubleValue();
                        double settlePrice = formatBigDecimal(price.getChdSettlePrice()).doubleValue();
                        if (price.getChdFloatPriceType() != null && price.getChdFloatPriceType() == 1) {
                            price.setChdSellPrice(formatBigDecimalInt(BigDecimal.valueOf(BigDecimalUtil.add(settlePrice, floatPrice))));
                        } else if (price.getChdFloatPriceType() != null && price.getChdFloatPriceType() == 2) {
                            price.setChdSellPrice(formatBigDecimalInt(BigDecimal.valueOf(BigDecimalUtil.add(settlePrice, BigDecimalUtil.mul(settlePrice, floatPrice == 0 ? 1 : floatPrice)))));
                        }
                    } else if (supplierPolicy != null) {
                        // 政策加价计算
                        BigDecimal newPrice = formatBigDecimalInt(BigDecimal.valueOf((Double) se.eval(supplierPolicy.getPriceFormula().replace("price",
                                price.getChdSellPrice().toPlainString()))));
                        price.setChdSellPrice(newPrice);
                    }
                }
            }
            log.info("加价完成，加价后价格={}", JSON.toJSONString(increasePrice.getPrices()));
        } catch (Exception e) {
            log.error("加价计算失败，不影响主流程，channel = {}, productCode = {}", increasePrice.getChannelCode(), increasePrice.getProductCode(), e);
        }
    }

    private SupplierPolicyPO getPolicy(IncreasePrice increasePrice){
        List<SupplierPolicyPO> supplierPolices = supplierPolicyDao.getSupplierPolicy(increasePrice);
        if(ListUtils.isEmpty(supplierPolices)){
            log.info("没有查到相关加价配置");
            return null;
        }
        log.info("查到的加价配置={}", JSON.toJSONString(supplierPolices));
        ScriptEngine se = new ScriptEngineManager().getEngineByName("JavaScript");
        IncreasePriceCalendar priceSample = increasePrice.getPrices().stream().filter(p ->
                p.getAdtSellPrice() != null && p.getAdtSellPrice().compareTo(new BigDecimal(0)) == 1).findFirst().orElse(null);
        SupplierPolicyPO supplierPolicy = supplierPolices.stream().filter(sp -> {
            try {
                se.eval(sp.getPriceFormula().replace("price",
                        priceSample.getAdtSellPrice().toPlainString()));
                return true;
            } catch (Exception e) {
                log.error("加价公式错误，id={}, formula={}", sp.getId(), sp.getPriceFormula(), e);
                return false;
            }
        }).sorted(Comparator.comparing(sp -> {
            try {
                return (Double) se.eval(sp.getPriceFormula().replace("price",
                        priceSample.getAdtSellPrice().toPlainString()));
            } catch (Exception e) {
                log.error("加价公式错误，id={}, formula={}", sp.getId(), sp.getPriceFormula(), e);
                return Double.MAX_VALUE;
            }
        }, Double::compareTo)).findFirst().get();
        log.info("找到合适的加价配置，使用这条配置加价，配置={}", JSON.toJSONString(supplierPolicy));
        return supplierPolicy;
    }

    private BigDecimal formatBigDecimal(BigDecimal bigDecimal){
        return bigDecimal == null ? new BigDecimal(0) : bigDecimal;
    }

    private BigDecimal formatBigDecimalInt(BigDecimal bigDecimal){
        return bigDecimal == null ? new BigDecimal(0) : bigDecimal.setScale(0, BigDecimal.ROUND_HALF_UP);
    }

    @Override
    public void increasePriceByPackageId(IncreasePrice increasePrice){
        try {
            buildIncreasePrice(increasePrice);
        } catch (Exception e) {
            log.error("加价异常", e);
        }
    }

    private void buildIncreasePrice(IncreasePrice increasePrice){
        if(ListUtils.isEmpty(increasePrice.getPrices())){
            return;
        }
        increasePrice.getPrices().forEach(p -> {
            if(StringUtils.isBlank(p.getPackageId())){
                return;
            }
            if(StringUtils.equals(increasePrice.getProductCategory(), ProductCategoryEnum.PRODUCT_CATEGORY_TICKET.getCode())){
                ScenicSpotProductPriceMPO price = scenicSpotProductPriceDao.getPriceById(p.getPackageId());
                if(price == null){
                    return;
                }
                p.setAdtFloatPrice(price.getFloatPrice());
                p.setAdtFloatPriceManually(price.isFloatPriceManually());
                p.setAdtFloatPriceType(price.getFloatPriceType());
                p.setAdtSellPrice(price.getSellPrice());
                p.setAdtSettlePrice(price.getSettlementPrice());
                increasePrice(increasePrice);
            } else if(StringUtils.equals(increasePrice.getProductCategory(), ProductCategoryEnum.PRODUCT_CATEGORY_GROUP_TOUR.getCode())){
                GroupTourProductSetMealMPO setMealMPO = groupTourProductSetMealDao.getSetMealById(p.getPackageId());
                if(setMealMPO == null || ListUtils.isEmpty(setMealMPO.getGroupTourPrices())){
                    return;
                }
                GroupTourPrice price = setMealMPO.getGroupTourPrices().stream().filter(gp ->
                        StringUtils.equals(p.getDate(), gp.getDate())).findFirst().orElse(null);
                if(price == null){
                    return;
                }
                p.setAdtFloatPrice(price.getAdtFloatPrice());
                p.setAdtFloatPriceManually(price.isAdtFloatPriceManually());
                p.setAdtFloatPriceType(price.getAdtFloatPriceType());
                p.setAdtSellPrice(price.getAdtSellPrice());
                p.setAdtSettlePrice(price.getAdtPrice());

                p.setChdFloatPrice(price.getChdFloatPrice());
                p.setChdSettlePrice(price.getChdPrice());
                p.setChdSellPrice(price.getChdSellPrice());
                p.setChdFloatPriceManually(price.isChdFloatPriceManually());
                p.setChdFloatPriceType(price.getChdFloatPriceType());
                increasePrice(increasePrice);
            } else if(StringUtils.equals(increasePrice.getProductCategory(), ProductCategoryEnum.PRODUCT_CATEGORY_HOTEL_TICKET.getCode())){
                HotelScenicSpotProductSetMealMPO setMealMPO = hotelScenicSpotProductSetMealDao.getSetMealById(p.getPackageId());
                if(setMealMPO == null || ListUtils.isEmpty(setMealMPO.getPriceStocks())){
                    return;
                }
                HotelScenicSpotPriceStock price = setMealMPO.getPriceStocks().stream().filter(gp ->
                        StringUtils.equals(p.getDate(), gp.getDate())).findFirst().orElse(null);
                if(price == null){
                    return;
                }
                p.setAdtFloatPrice(price.getAdtFloatPrice());
                p.setAdtFloatPriceManually(price.isAdtFloatPriceManually());
                p.setAdtFloatPriceType(price.getAdtFloatPriceType());
                p.setAdtSellPrice(price.getAdtSellPrice());
                p.setAdtSettlePrice(price.getAdtPrice());

                p.setChdFloatPrice(price.getChdFloatPrice());
                p.setChdSettlePrice(price.getChdPrice());
                p.setChdSellPrice(price.getChdSellPrice());
                p.setChdFloatPriceManually(price.isChdFloatPriceManually());
                p.setChdFloatPriceType(price.getChdFloatPriceType());
                increasePrice(increasePrice);
            }
        });
    }

    @Override
    @Transactional
    public void sendCouponDelivery(CouponSendParam couponSendParam) throws Exception {
        CouponSuccess couponSuccess = new CouponSuccess();
        try {
            couponSuccess = couponDeliveryService.sendCouponDelivery(couponSendParam);
        } catch (Exception e) {
            log.error("发券异常:", e);
            throw new RuntimeException("发券异常");
        }
        log.info("CouponSuccess:{}", JSONObject.toJSONString(couponSuccess));
        if (couponSuccess == null || !couponSuccess.getCode().equals("0")) {
            throw new RuntimeException("发券异常");
        }
    }
}
