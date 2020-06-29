package com.huoli.trip.central.web;

import com.alibaba.dubbo.config.spring.context.annotation.DubboComponentScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@DubboComponentScan(value = "com.huoli.trip.central.web")
@ComponentScan({"com.huoli.trip"})
public class HltripCentralApplication {

    public static void main(String[] args) {
        SpringApplication.run(HltripCentralApplication.class, args);
    }

}
