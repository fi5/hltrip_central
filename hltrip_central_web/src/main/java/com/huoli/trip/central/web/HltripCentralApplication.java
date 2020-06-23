package com.huoli.trip.central.web;

import com.alibaba.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@DubboComponentScan(value = "com.huoli.trip.central.web.service")
public class HltripCentralApplication {

    public static void main(String[] args) {
        SpringApplication.run(HltripCentralApplication.class, args);
    }

}
