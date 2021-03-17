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
import java.util.Comparator;
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
    @Scheduled(cron = "0 0/25 * * * ?")
    public void refreshRecommendList(){
        refreshRecommendList(0);
    }

    @Async
    @PostConstruct
    @Scheduled(cron = "0 0/3 * * * ?")
    public void refreshRecommendListV2(){
        refreshRecommendListV2(0);
    }


    @Async
    public void refreshRecommendList(int force) {
        if ((schedule == null || !StringUtils.equalsIgnoreCase("yes", schedule)) && force != 1) {
            return;
        }
        log.info("执行刷新推荐列表任务。。。");
        List<Integer> all = Arrays.asList(ProductType.values()).stream().map(t -> t.getCode())
                .filter(t -> t != ProductType.UN_LIMIT.getCode()).collect(Collectors.toList());
        for (Integer t : all) {
            String key = String.join("", RECOMMEND_LIST_FLAG_TYPE_KEY_PREFIX, t.toString());
            List<ProductPO> productPOs = productDao.getFlagRecommendResult_(t, 4);
            if (ListUtils.isNotEmpty(productPOs)) {
                log.info("类型{}有。。", t);
                redisService.set(key,
                        JSON.toJSONString(ProductConverter.convertToProducts(productPOs, 0)), 1, TimeUnit.DAYS);
            } else {
                log.info("类型{}没有。。", t);
                jedisTemplate.delete(key);
            }
        }
    }
    /**
     * 刷新推荐列表缓存
     * @param force 强制刷新
     */
    @Async
    public void refreshRecommendListV2(int force){
        if((schedule == null || !StringUtils.equalsIgnoreCase("yes", schedule)) && force != 1){
            return;
        }
        log.info("执行刷新推荐列表任务。。。");
        List<RecommendProductPO> recommendProductPOs = productDao.getRecommendProducts();
        if(ListUtils.isNotEmpty(recommendProductPOs)){
            log.info("数据库的推荐产品={}", JSON.toJSONString(recommendProductPOs));
            List<RecommendProductPO> recommends = recommendProductPOs.stream().map(r -> {
                log.info("检查产品{}", r.getProductCode());
                ProductPO productPO = productDao.getTripProductByCode(r.getProductCode());
                if(productPO.getStatus() != Constants.PRODUCT_STATUS_VALID){
                    log.info("产品{}非上线状态{}，跳过。", productPO.getCode(), productPO.getStatus());
                     productDao.updateRecommendProductStatus(productPO.getCode(), productPO.getStatus());
                     return null;
                }
                PriceSinglePO priceSinglePO = priceDao.selectByProductCode(productPO.getCode());
                r.setPriceInfo(priceSinglePO.getPriceInfos());
                log.info("产品{}最新价格={}", productPO.getCode(), JSON.toJSONString(r.getPriceInfo()));
                return r;
            }).filter(r -> r != null).collect(Collectors.toList());
            int size = ConfigGetter.getByFileItemInteger(ConfigConstants.CONFIG_FILE_NAME_COMMON, CentralConstants.CONFIG_RECOMMEND_SIZE);
            recommends.stream().collect(Collectors.groupingBy(RecommendProductPO::getPosition)).forEach((k, v) -> {
                v.sort(Comparator.comparing(p -> p.getLevel(), Integer::compareTo));
                if(v.size() > size){
                    v = v.subList(0, size);
                }
                List<String> ids = v.stream().map(r -> r.getId()).collect(Collectors.toList());
                productDao.updateRecommendDisplay(ids, Constants.RECOMMEND_DISPLAY_YES, k);
                productDao.updateRecommendNotDisplay(ids, k);
                String key = String.join("_", RECOMMEND_LIST_POSITION_KEY_PREFIX, k.toString());
                redisService.set(key,
                        JSON.toJSONString(v), 1, TimeUnit.DAYS);
                log.info("缓存{}的产品{}", key, JSON.toJSONString(v));
            });
        } else {
            log.error("没有可用推荐产品了。");
        }
    }
}
