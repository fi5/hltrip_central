package com.huoli.trip.central.web.config;

import brave.Tracer;
import com.google.common.collect.Maps;
import com.huoli.eagle.BraveTrace;
import com.huoli.eagle.eye.core.HuoliAtrace;
import com.huoli.eagle.eye.core.HuoliTrace;
import com.huoli.eagle.eye.core.statistical.trace.TraceInfo;
import com.huoli.eagle.report.SleuthSpanESReporter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Map;


@Configuration
@PropertySource("classpath:atrace.properties")
public class TraceConfig {

    @Value("${dptCode}")
    private String dptCode;
    @Value("${bizCode}")
    private String bizCode;
    @Value("${appName}")
    private String appName;



    @Bean
    public HuoliAtrace huoliAtrace(HuoliTrace huoliTrace) {
        HuoliAtrace huoliAtrace = new HuoliAtrace.Builder()
                .withDptCode(dptCode)
                .withBizCode(bizCode)
                .withAppname(appName)
                .withHuoliTrace(huoliTrace) // optional
                .build();
        return huoliAtrace;
    }


    @Bean
    public BraveTrace huoliTrace(Tracer tracer) {
        return new BraveTrace(tracer);
    }
    @Bean
    public SleuthSpanESReporter sleuthSpanESReporter() {
        return new SleuthSpanESReporter();
    }

    @SuppressWarnings("unchecked")
    public static Object createSpan(String name, HuoliTrace huoliTrace) {
        Object newSpan;
        Object currentSpan = huoliTrace.currentSpan();
        if (currentSpan != null) {
            newSpan = huoliTrace.createSpan(name, currentSpan);
        } else {
            newSpan = huoliTrace.createSpan(name);
        }
        return newSpan;
    }

    public static Map<String, String> traceHeaders(HuoliTrace huoliTrace, String url) {
        Map<String, String> headers = Maps.newHashMap();
        TraceInfo traceInfo = huoliTrace.getTraceInfo();
        if (traceInfo != null) {
            headers.put("X-B3-TraceId", traceInfo.getTraceId());
            headers.put("X-B3-SpanId", traceInfo.getSpanId());
            if (traceInfo.getParentSpanId() != null) {
                headers.put("X-B3-ParentSpanId", traceInfo.getParentSpanId());
            }
            huoliTrace.setUrl4Name(url);
            return headers;
        }
        return null;
    }
}
