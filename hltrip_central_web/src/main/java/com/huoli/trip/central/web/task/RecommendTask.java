package com.huoli.trip.central.web.task;

import com.alibaba.fastjson.JSON;
import com.huoli.trip.central.web.constant.CentralConstants;
import com.huoli.trip.central.web.converter.ProductConverter;
import com.huoli.trip.central.web.dao.PriceDao;
import com.huoli.trip.central.web.dao.ProductDao;
import com.huoli.trip.central.web.service.RedisService;
import com.huoli.trip.common.constant.ConfigConstants;
import com.huoli.trip.common.constant.Constants;
import com.huoli.trip.common.constant.ProductType;
import com.huoli.trip.common.entity.PriceSinglePO;
import com.huoli.trip.common.entity.ProductPO;
import com.huoli.trip.common.entity.RecommendProductPO;
import com.huoli.trip.common.util.ConfigGetter;
import com.huoli.trip.common.util.ListUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.huoli.trip.central.web.constant.CentralConstants.*;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2020/10/23<br>
 */
@Component
@Slf4j
public class RecommendTask {

    @Value("${schedule.executor}")
    private String schedule;

    @Autowired
    private ProductDao productDao;

    @Autowired
    private RedisService redisService;

    @Autowired
    private RedisTemplate jedisTemplate;

    @Autowired
    private PriceDao priceDao;

    @Async
    @PostConstruct
    @Scheduled(cron = "0 0/15 * * * ?")
    public void refreshRecommendList(){
        refreshRecommendList(0);
    }

    /**
     * 刷新推荐列表缓存
     * @param force 强制刷新
     */
    @Async
    public void refreshRecommendList(int force){
        if((schedule == null || !StringUtils.equalsIgnoreCase("yes", schedule)) && force != 1){
            return;
        }
        log.info("执行刷新推荐列表任务。。。");
        List<RecommendProductPO> recommendProductPOs = productDao.getRecommendProducts();
        if(ListUtils.isNotEmpty(recommendProductPOs)){
            List<RecommendProductPO> recommends = recommendProductPOs.stream().map(r -> {
                ProductPO productPO = productDao.getTripProductByCode(r.getProductCode());
                if(productPO.getStatus() != Constants.PRODUCT_STATUS_VALID){
                     productDao.updateRecommendProductStatus(productPO.getCode(), productPO.getStatus());
                     return null;
                }
                PriceSinglePO priceSinglePO = priceDao.selectByProductCode(productPO.getCode());
                productPO.setPriceCalendar(priceSinglePO);
                return r;
            }).filter(r -> r != null).collect(Collectors.toList());
            recommends.stream().collect(Collectors.groupingBy(RecommendProductPO::getPosition)).forEach((k, v) -> {
                if(v.size() > 3){
                    v = v.subList(0, ConfigGetter.getByFileItemInteger(ConfigConstants.CONFIG_FILE_NAME_COMMON, CentralConstants.CONFIG_RECOMMEND_SIZE));
                }
                String key = String.join("_", RECOMMEND_LIST_POSITION_KEY_PREFIX, k.toString());
                redisService.set(key,
                        JSON.toJSONString(v), 1, TimeUnit.DAYS);
            });
        } else {
            log.error("没有可用推荐产品了。");
        }
    }
}
