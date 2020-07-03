package com.huoli.trip.central.web.aop;

import com.alibaba.fastjson.JSON;
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
public class CentralServiceAspect {

    @Pointcut(value = "execution(* com.huoli.trip.central.web.service..*ServiceImpl.*(..))")
    public void apiPointCut() {
    }

    @Around(value = "apiPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String function = joinPoint.getSignature().getName();
        Object args[] = joinPoint.getArgs();
        Object result;
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
            throw e;
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
