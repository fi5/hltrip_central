package com.huoli.trip.central.web.util;

import com.huoli.eagle.eye.core.HuoliAtrace;
import com.huoli.eagle.eye.core.HuoliTrace;
import com.huoli.eagle.eye.core.statistical.trace.TraceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Component
@Slf4j
public class TraceIdUtils {
    private static HuoliAtrace huoliAtrace;

    @Autowired
    public TraceIdUtils(HuoliAtrace _huoliAtrace) {
        huoliAtrace = _huoliAtrace;
    }

    public static String getTraceId() {
        String traceId = "test";
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            traceId = request.getHeader("X-B3-TraceId");
            log.info("X-B3-TraceId is :{}",traceId);
        } catch (Exception e) {
            log.info("获取traceID 失败");
        }
        if (StringUtils.isEmpty(traceId)) {
            HuoliTrace huoliTrace = huoliAtrace.getHuoliTrace();
            if (huoliTrace != null) {
                TraceInfo traceInfo = huoliTrace.getTraceInfo();
                if (traceInfo != null) {
                    traceId = traceInfo.getTraceId();
                    log.info("重新生成 traceId:{}",traceId);
                }
            }
        }
        return traceId;
    }

}
