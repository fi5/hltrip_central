package com.huoli.trip.central.web.task;

import com.alibaba.fastjson.JSON;
import com.huoli.trip.central.web.converter.ProductConverter;
import com.huoli.trip.central.web.dao.ProductDao;
import com.huoli.trip.central.web.service.RedisService;
import com.huoli.trip.common.constant.ProductType;
import com.huoli.trip.common.entity.ProductPO;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.huoli.trip.central.web.constant.Constants.RECOMMEND_LIST_FLAG_TYPE_KEY_PREFIX;

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

    @Async
    @PostConstruct
    @Scheduled(cron = "0 0/15 * * * ?")
    public void refreshRecommendList(){
        if(schedule == null || !StringUtils.equalsIgnoreCase("yes", schedule)){
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
}
