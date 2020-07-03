package com.huoli.trip.central.web.aop;

import com.alibaba.fastjson.JSON;
import com.huoli.trip.common.exception.HlCentralException;
import com.huoli.trip.common.vo.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 描述：desc<br>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：顾刘川<br>
 * 版本：1.0<br>
 * 创建日期：2020/7/3<br>
 */
@Component
@Aspect
@Slf4j
public class CentralDubboServiceAspect {

    @Pointcut("@within(com.alibaba.dubbo.config.annotation.Service)")
    public void apiPointCut() {
    }

    @Around(value = "apiPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String function = joinPoint.getSignature().getName();
        Object args[] = joinPoint.getArgs();
        Object result = null;
        StopWatch stopWatch = new StopWatch();
        Object argsCopy[] = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object argu = args[i];
            if (argu instanceof HttpServletResponse) {
                continue;
            }
            if (argu instanceof HttpServletRequest) {
                continue;
            }
        }
        stopWatch.start();
        try {
            log.info("[{}] request: {}", function, JSON.toJSON(args));
            result = joinPoint.proceed(args);
        } catch (Throwable e) {
            if(e instanceof HlCentralException){
                HlCentralException hlCentralException = (HlCentralException) e;
                log.error("[{}],e",function,hlCentralException);
                result= BaseResponse.withFail(hlCentralException.getCode(),hlCentralException.getMessage());
            }
        } finally {
            stopWatch.stop();
        }
        if (result != null) {
            log.info("[{}],response: {}, cost: {},", function, JSON.toJSON(result),
                    stopWatch.getTotalTimeMillis());
        }
        return result;
    }

}
