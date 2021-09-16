package com.huoli.trip.central.web.service.impl;

import com.alibaba.fastjson.JSON;
import com.huoli.trip.central.web.dao.SupplierPolicyDao;
import com.huoli.trip.central.web.service.CommonService;
import com.huoli.trip.common.entity.SupplierPolicyPO;
import com.huoli.trip.common.util.BigDecimalUtil;
import com.huoli.trip.common.util.ListUtils;
import com.huoli.trip.common.vo.IncreasePrice;
import com.huoli.trip.common.vo.IncreasePriceCalendar;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
                if(price.isAdtFloatPriceManually()){
                    if(price.getAdtFloatPriceType() != null && price.getAdtFloatPriceType() == 1){
                        price.setAdtSellPrice(BigDecimal.valueOf(BigDecimalUtil.add(formatBigDecimal(price.getAdtSettlePrice()).doubleValue()
                                , formatBigDecimal(price.getAdtFloatPrice()).doubleValue())));
                    } else if(price.getAdtFloatPriceType() != null && price.getAdtFloatPriceType() == 2){

                    }

                } else if(supplierPolicy != null){
                    // 加价计算
                    if(price.getAdtSettlePrice() != null){
                        BigDecimal newPrice = BigDecimal.valueOf((Double) se.eval(supplierPolicy.getPriceFormula().replace("price",
                                price.getAdtSettlePrice().toPlainString()))).setScale(0, BigDecimal.ROUND_HALF_UP);
                        price.setAdtSellPrice(newPrice);
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
}
