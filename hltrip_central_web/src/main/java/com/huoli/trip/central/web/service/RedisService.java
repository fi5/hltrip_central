package com.huoli.trip.central.web.service;

import java.util.concurrent.TimeUnit;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2020/9/27<br>
 */
public interface RedisService {

    /**
     * 设置缓存数据
     * @param key
     * @param value
     * @param expireTime
     * @param timeUnit
     * @return
     */
    boolean setNx(String key, String value, long expireTime, TimeUnit timeUnit);

    /**
     * 设置缓存数据（覆盖）
     * @param key
     * @param value
     * @param expireTime
     * @param timeUnit
     */
    void set(String key, String value, long expireTime, TimeUnit timeUnit);

    /**
     * 删除缓存
     * @param key
     * @return
     */
    boolean del(String key);

    /**
     * 递增
     * @param key
     * @return
     */
    Long incr(String key);

    /**
     * 递减
     * @param key
     * @return
     */
    Long decr(String key);

    /**
     * 检查缓存
     * @param key
     * @return
     */
    Long ttl(String key);

    /**
     * 设置过期时间（毫秒）
     * @param key
     * @param timeout ms
     * @return
     */
    Boolean pExpire(String key, long timeout);

    /**
     * 计数器++
     * @param key
     * @return
     */
    long countIncr(String key);

    /**
     * 计数器--
     * @param key
     * @return
     */
    long countDecr(String key);
}
