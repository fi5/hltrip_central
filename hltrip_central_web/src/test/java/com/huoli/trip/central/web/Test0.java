package com.huoli.trip.central.web;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.huoli.trip.central.web.dao.ProductDao;
import com.huoli.trip.central.web.util.DateUtils;
import com.huoli.trip.common.entity.PriceInfoPO;
import com.huoli.trip.common.entity.PricePO;
import com.huoli.trip.common.entity.ProductItemPO;
import com.huoli.trip.common.entity.ProductPO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/1<br>
 */
@SpringBootTest
@Slf4j
public class Test0 {
    @Autowired
    private ProductDao productDao;
    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    public void test(){
        List<String> ids = Lists.newArrayList("11", "22","33","44","55","66","77");
        List<ProductItemPO> list = productDao.getProductListByItemIdsPage(ids, 1, 3);
        log.info("结果 = {}", JSON.toJSONString(list));
    }

    @Test
    public void testPriceInfos() throws Exception {
        String productCode = "yaochufa_247533_266960";
        String begin = "2020-07-03";
        String end = "2020-07-05";
        if(StringUtils.isBlank(end)){
            end = begin;
        }
        Date date1 = DateUtils.parseTimeStringToDate2(begin);
        Date date2 = DateUtils.parseTimeStringToDate2(end);
//        Document queryObject = new Document();
//        queryObject.put("productCode", productCode);
        PricePO pricePos = productDao.getPricePos(productCode);
        if(pricePos!=null){
            List<PriceInfoPO> priceInfos = pricePos.getPriceInfos();
            System.out.println(priceInfos);
            List<PriceInfoPO> collect = priceInfos.stream().filter(s -> (s.getSaleDate().compareTo(date1)==0||s.getSaleDate().compareTo(date1)==1)
                    &&(s.getSaleDate().compareTo(date2)==0||s.getSaleDate().compareTo(date2)==-1)).collect(Collectors.toList());
            System.out.println(collect);
        }
//        Document fieldsObject = new Document();
//        fieldsObject.put("productCode", false);
//        fieldsObject.put("priceInfos", true);
//
//        Query query = new BasicQuery(queryObject, fieldsObject);

//        PricePO pricePO = mongoTemplate.findOne(query, PricePO.class);
//        System.out.println(pricePos);
//        List<PriceInfoPO> comments = Optional.of(pricePO).orElseThrow(Exception::new).getPriceInfos();
//        System.out.println(comments);
    }

}
