package com.huoli.trip.central.web;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.huoli.trip.central.web.dao.ProductDao;
import com.huoli.trip.central.web.dao.impl.TestPO;
import com.huoli.trip.common.entity.ProductPO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

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

    @Test
    public void test(){
        List<String> ids = Lists.newArrayList("11", "22","33","44","55","66","77");
        List<ProductPO> list = productDao.getProductListByItemIdsPage(ids, 2, 3);
        log.info("结果 = {}", JSON.toJSONString(list));
    }
}
