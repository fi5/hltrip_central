package com.huoli.trip.central.web.config;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
public class DubboConfig {

    @Resource
    private DubboProperties dubboProperties;

    /**
     * 应用名配置，等同于 <dubbo:application name="xxx"  />
     *
     * @return ApplicationConfig
     */
    @Bean
    public ApplicationConfig applicationConfig() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName(dubboProperties.getName());
        return applicationConfig;
    }

    /**
     * 注册中心配置，等同于 <dubbo:registry address="url" />
     *
     * @return RegistryConfig
     */
    @Bean
    public RegistryConfig registryConfig() {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress(dubboProperties.getAddress());
        registryConfig.setClient(dubboProperties.getClient());
        registryConfig.setTimeout(10000);
        registryConfig.setCheck(false);
        return registryConfig;
    }

    /**
     * 协议配置，等同于 <dubbo:protocol name="dubbo" port="20880" />
     *
     * @return ProtocolConfig
     */
    @Bean
    public ProtocolConfig protocolConfig() {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setName(dubboProperties.getProtocolName());
        protocolConfig.setPort(dubboProperties.getProtocolPort());
        return protocolConfig;
    }

    @Bean
    public ReferenceConfig referenceConfig(){
        ReferenceConfig referenceConfig = new ReferenceConfig();
        referenceConfig.setCheck(false);
        return referenceConfig;
    }
}
