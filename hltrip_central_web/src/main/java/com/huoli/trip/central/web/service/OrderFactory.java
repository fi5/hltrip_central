package com.huoli.trip.central.web.service;

import com.google.common.collect.Maps;
import com.huoli.trip.central.web.service.impl.OrderManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * 描述：desc<br>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：顾刘川<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/1<br>
 */
@Component
@Slf4j
public class OrderFactory {

    @Autowired
    private ApplicationContext applicationContext;

    public static Map<String,OrderManager> orderManagerMap = Maps.newHashMap();
    @PostConstruct
    public void init(){
        log.info("init order manager.....");
        register();
    }

    /**
     * 注册服务
     */
    private void register(){
       Map<String, OrderManager> managerMap = applicationContext.getBeansOfType(OrderManager.class);
      for (String str:managerMap.keySet()){
          OrderManager orderManager =  managerMap.get(str);
          orderManagerMap.put(orderManager.getChannel(),orderManager);
      }
    }

    /**
     * 根据渠道获取OrderManager
     * @param channel
     * @return
     */
    public  OrderManager getOrderManager(String channel){
        return orderManagerMap.get(channel);
    }

}
