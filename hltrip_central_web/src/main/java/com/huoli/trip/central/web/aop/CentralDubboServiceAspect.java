package com.huoli.trip.central.web.aop;

import brave.Span;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.huoli.eagle.BraveTrace;
import com.huoli.eagle.eye.core.HuoliAtrace;
import com.huoli.eagle.eye.core.statistical.Event;
import com.huoli.eagle.eye.core.statistical.EventStatusEnum;
import com.huoli.trip.central.web.config.TraceConfig;
import com.huoli.trip.common.constant.CentralError;
import com.huoli.trip.common.exception.HlCentralException;
import com.huoli.trip.common.vo.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.remoting.RemotingException;
import org.apache.dubbo.rpc.RpcException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.validation.ValidationException;
import java.util.Objects;
import java.util.stream.Stream;

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
    private BraveTrace huoliTrace;
    /**
     * 事件上报处理类
     */
    @Autowired
    private HuoliAtrace huoliAtrace;

    @Pointcut("@within(org.apache.dubbo.config.annotation.Service)")
    public void apiPointCut() {
    }

    @Around(value = "apiPointCut()")
    public Object around(ProceedingJoinPoint joinPoint){
        String function = joinPoint.getSignature().getName();
        Event.EventBuilder eventBuilder = new Event.EventBuilder();
        eventBuilder.withData("method", function);
        eventBuilder.withIndex(huoliAtrace.getAppname(), "service");

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Span span = null;
        try {
            Object args[] = joinPoint.getArgs();
            this.paramValidate(args);
            Object result;
            String params;
            if(ArrayUtils.isNotEmpty(args) && args[0] != null){
                try {
                    params = JSON.toJSONString(args[0]);
                    JSONObject param = JSONObject.parseObject(params);
                    if(StringUtils.isBlank(param.getString("traceId"))){
                        log.error("方法 {} 参数不包含traceId", function);
                    } else {
                        // 设置traceId
                        span = (Span) TraceConfig.createSpan(function, this.huoliTrace, param.getString("traceId"));
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
                result = joinPoint.proceed(args);
                eventBuilder.withData("code", 0);
                eventBuilder.withStatus(EventStatusEnum.SUCCESS);
            } catch (HlCentralException e) {
                log.error("[{}] 业务异常: ", function, e);
                result = BaseResponse.withFail(e.getCode(), e.getMessage(), e.getData());
                eventBuilder.withData("code", e.getCode());
                eventBuilder.withStatus(EventStatusEnum.FAIL);
            } catch (RpcException | RemotingException e){
                // 是Dubbo本身的异常，直接抛出
                log.error("[{}] duboo服务不可用: ", function, e);
                result = BaseResponse.withFail(CentralError.DUBOO_RPC_ERROR);
                eventBuilder.withData("code", CentralError.DUBOO_RPC_ERROR.getCode());
                eventBuilder.withStatus(EventStatusEnum.FAIL);
            } catch (NullPointerException e){
                log.error("[{}] 数据不完整异常: ", function, e);
                result = BaseResponse.withFail(CentralError.DATA_NULL_ERROR);
                eventBuilder.withData("code", CentralError.DATA_NULL_ERROR.getCode());
                eventBuilder.withStatus(EventStatusEnum.FAIL);
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
            // 这个方法日志量过大
            if(!function.equals("recommendList")){
                log.info("[{}], response: {}, cost: {},", function, JSON.toJSONString(result),
                        stopWatch.getTotalTimeMillis());
            }
            return result;
        } catch (ValidationException | TypeMismatchException e){
            log.error("[{}] 请求参数异常: ", function, e);
            eventBuilder.withData("code", CentralError.ERROR_BAD_REQUEST.getCode());
            eventBuilder.withStatus(EventStatusEnum.FAIL);
            String result = String.format("%s : %s", CentralError.ERROR_BAD_REQUEST.getError(), e.getMessage());
            return BaseResponse.withFail(CentralError.ERROR_BAD_REQUEST.getCode(), result);
        } catch (Throwable e) {
            eventBuilder.withData("code", CentralError.ERROR_UNKNOWN.getCode());
            eventBuilder.withStatus(e);
            log.error("切面执行异常：", e);
            return BaseResponse.fail(CentralError.ERROR_UNKNOWN);
        } finally {
            Event event = eventBuilder.build();
            huoliAtrace.reportEvent(event);
            if(span != null){
                this.huoliTrace.close(span);
            }
        }
    }

    /**
     * 参数校验
     * @param params
     */
    private void paramValidate(Object[] params) {
        if (ArrayUtils.isEmpty(params)) {
            return;
        }
        Stream.of(params).forEach(param -> {
            if (Objects.isNull(param)) {
                throw new ValidationException("传入参数为空！");
            }
            ValidatorUtil.validate(param);
        });
    }
}
