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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.huoli.trip.central.web.constant.CentralConstants.RECOMMEND_LIST_FLAG_TYPE_KEY_PREFIX;

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
            // todo 推荐流程需要确定，优先级、推荐位置、产品类型这些维度的组合，如果实时查的话，流程需要重新定，现有的流程是否需要兼容
            recommends.stream().collect(Collectors.groupingBy(RecommendProductPO::getProductType)).forEach((k, v) -> {
                v.stream().collect(Collectors.groupingBy(RecommendProductPO::getPosition)).forEach((k1, v1) -> {
                    String key = String.join("_", RECOMMEND_LIST_FLAG_TYPE_KEY_PREFIX, k.toString(), k1.toString());
                });
            });
        } else {
            List<Integer> all = Arrays.asList(ProductType.values()).stream().map(t -> t.getCode())
                    .filter(t -> t != ProductType.UN_LIMIT.getCode()).collect(Collectors.toList());
            for (Integer t : all) {
                String key = String.join("", RECOMMEND_LIST_FLAG_TYPE_KEY_PREFIX, t.toString());
                Integer size = ConfigGetter.getByFileItemInteger(ConfigConstants.CONFIG_FILE_NAME_COMMON, CentralConstants.CONFIG_RECOMMEND_SIZE);
                List<ProductPO> productPOs = productDao.getFlagRecommendResult_(t, size == null ? 3 : size);
                if (ListUtils.isNotEmpty(productPOs)) {
                    // 先把当前类型所有都置为不展示
//                productDao.updateRecommendDisplay(null, Constants.RECOMMEND_DISPLAY_NO, t);
//                // 把查出来的设置成展示
//                productDao.updateRecommendDisplay(productPOs.stream().map(ProductPO::getCode).collect(Collectors.toList()),
//                        Constants.RECOMMEND_DISPLAY_YES, t);
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

}
