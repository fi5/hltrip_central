package com.huoli.trip.central.web.aop;

import com.alibaba.fastjson.JSON;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.exception.HlCentralException;
import com.huoli.trip.common.vo.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.TypeMismatchException;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.MethodArgumentNotValidException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ValidationException;

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
    public Object around(ProceedingJoinPoint joinPoint){
        try {
            String function = joinPoint.getSignature().getName();
            Object args[] = joinPoint.getArgs();
            Object result;
            StopWatch stopWatch = new StopWatch();
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
            } catch (HlCentralException e) {
                log.error("[{}] 业务异常: ", function, e);
                result = BaseResponse.withFail(e.getCode(), e.getMessage(), e.getData());
            } catch (ValidationException | TypeMismatchException | MethodArgumentNotValidException e){
                log.error("[{}] 请求参数异常: ", function, e);
                result = BaseResponse.withFail(CentralError.ERROR_BAD_REQUEST);
            } catch (NullPointerException e) {
                log.error("[{}] 数据不完整异常: ", function, e);
                result = BaseResponse.withFail(CentralError.DATA_NULL_ERROR);
            } catch  (Throwable e) {
                log.error("[{}] 服务器内部错误异常: ", function, e);
                result = BaseResponse.withFail(CentralError.ERROR_SERVER_ERROR);
            } finally {
                stopWatch.stop();
            }
            if (result == null) {
                log.error("[{}] result 为空", function);
                result = BaseResponse.fail(CentralError.ERROR_UNKNOWN);
            }
            log.info("[{}], response: {}, cost: {},", function, JSON.toJSON(result),
                    stopWatch.getTotalTimeMillis());
            return result;
        } catch (Throwable e) {
            log.error("切面执行异常：", e);
            return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
        }
    }
}
