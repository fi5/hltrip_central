package com.huoli.trip.central.web.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import java.io.Serializable;

@Data
public class DubboProperties implements Serializable {
    @Value("${dubbo.server.name}")
    private String name;
    @Value("${dubbo.server.address}")
    private String address;
    @Value("${dubbo.server.client}")
    private String client;
    @Value("${dubbo.server.protocolName}")
    private String protocolName;
    @Value("${dubbo.server.protocolPort}")
    private Integer protocolPort;

}
