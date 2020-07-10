package com.huoli.trip.central.web;

import com.alibaba.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication(exclude = {RedisAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class,
        DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class,
        MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@ImportResource({"classpath:disconf-config.xml"})
@DubboComponentScan(value = "com.huoli.trip.central.web")
@ComponentScan({"com.huoli.trip"})
public class HltripCentralApplication {

    public static void main(String[] args) {
        SpringApplication.run(HltripCentralApplication.class, args);
    }

}
