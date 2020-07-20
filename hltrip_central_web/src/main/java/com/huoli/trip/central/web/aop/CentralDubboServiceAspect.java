package com.huoli.trip.central.web.aop;

import brave.Span;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.huoli.eagle.eye.core.HuoliAtrace;
import com.huoli.eagle.eye.core.HuoliTrace;
import com.huoli.eagle.eye.core.statistical.Event;
import com.huoli.eagle.eye.core.statistical.EventStatusEnum;
import com.huoli.trip.central.web.config.TraceConfig;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.exception.HlCentralException;
import com.huoli.trip.common.vo.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.MethodArgumentNotValidException;

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

    /**
     * 日志跟踪类
     */
    @Autowired
    private HuoliTrace huoliTrace;
    /**
     * 事件上报处理类
     */
    @Autowired
    private HuoliAtrace huoliAtrace;

    @Pointcut("@within(com.alibaba.dubbo.config.annotation.Service)")
    public void apiPointCut() {
    }

    @Around(value = "apiPointCut()")
    public Object around(ProceedingJoinPoint joinPoint){
        String function = joinPoint.getSignature().getName();
        Event.EventBuilder eventBuilder = new Event.EventBuilder();
        eventBuilder.withData("method", function);
        eventBuilder.withIndex(huoliAtrace.getAppname(), "service");
        Span span = (Span) TraceConfig.createSpan(function, this.huoliTrace);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            Object args[] = joinPoint.getArgs();
            Object result = null;
            String params;
            if(ArrayUtils.isNotEmpty(args) && args[0] != null){
                try {
                    params = JSON.toJSONString(args);
                    JSONObject param = JSONObject.parseObject(JSON.toJSONString(args[0]));
                    if(StringUtils.isBlank(param.getString("traceId"))){
                        log.error("方法 {} 参数不包含traceId", function);
                    } else {
                        // 设置traceId
                        TraceConfig.createNewSpan(param.getString("traceId"), span);
                    }
                } catch (Exception e) {
                    log.error("反序列化方法 {} 的请求参数异常，这是为了获取traceId，不影响主流程。", e);
                    params = "参数不能序列化";
                }
            } else {
                params = "无参数。";
            }
            try {
                log.info("[{}] request: {}", function, params);
                eventBuilder.withData("code", 0);
                eventBuilder.withStatus(EventStatusEnum.SUCCESS);
                result = joinPoint.proceed(args);
            } catch (HlCentralException e) {
                log.error("[{}] 业务异常: ", function, e);
                result = BaseResponse.withFail(e.getCode(), e.getMessage(), e.getData());
                eventBuilder.withData("code", e.getCode());
                eventBuilder.withStatus(EventStatusEnum.FAIL);
            } catch (ValidationException | TypeMismatchException | MethodArgumentNotValidException e){
                log.error("[{}] 请求参数异常: ", function, e);
                result = BaseResponse.withFail(CentralError.ERROR_BAD_REQUEST);
                eventBuilder.withData("code", CentralError.ERROR_BAD_REQUEST.getCode());
                eventBuilder.withStatus(EventStatusEnum.FAIL);
            }catch (Exception exception){
                // 是Dubbo本身的异常，直接抛出
                if (exception instanceof RpcException||exception instanceof RemotingException) {
                    log.error("[{}] duboo服务不可用: ", function, exception);
                    result = BaseResponse.withFail(CentralError.DUBOO_RPC_ERROR);
                    eventBuilder.withData("code", CentralError.DUBOO_RPC_ERROR.getCode());
                    eventBuilder.withStatus(EventStatusEnum.FAIL);
                }else if(exception instanceof NullPointerException){
                    log.error("[{}] 数据不完整异常: ", function, exception);
                    result = BaseResponse.withFail(CentralError.DATA_NULL_ERROR);
                    eventBuilder.withData("code", CentralError.DATA_NULL_ERROR.getCode());
                    eventBuilder.withStatus(EventStatusEnum.FAIL);
                }
            } catch  (Throwable e) {
                log.error("[{}] 服务器内部错误异常: ", function, e);
                result = BaseResponse.withFail(CentralError.ERROR_SERVER_ERROR);
                eventBuilder.withData("code", CentralError.ERROR_SERVER_ERROR.getCode());
                eventBuilder.withStatus(EventStatusEnum.FAIL);
            } finally {
                stopWatch.stop();
            }
            if (result == null) {
                log.error("[{}] result 为空", function);
                result = BaseResponse.fail(CentralError.ERROR_UNKNOWN);
                eventBuilder.withData("code", CentralError.ERROR_UNKNOWN.getCode());
                eventBuilder.withStatus(EventStatusEnum.FAIL);
            }
            log.info("[{}], response: {}, cost: {},", function, JSON.toJSONString(result),
                    stopWatch.getTotalTimeMillis());
            return result;
        } catch (Throwable e) {
            eventBuilder.withData("code", CentralError.ERROR_UNKNOWN.getCode());
            eventBuilder.withStatus(e);
            log.error("切面执行异常：", e);
            return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
        } finally {
            Event event = eventBuilder.build();
            huoliAtrace.reportEvent(event);
        }
    }
}
