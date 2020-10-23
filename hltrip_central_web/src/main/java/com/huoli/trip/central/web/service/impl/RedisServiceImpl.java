package com.huoli.trip.central.web.service.impl;

import com.huoli.trip.central.web.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 描述：<br/>
 * 版权：Copyright (c) 2011-2020<br>
 * 公司：活力天汇<br>
 * 作者：冯志强<br>
 * 版本：1.0<br>
 * 创建日期：2020/9/27<br>
 */
@Service
public class RedisServiceImpl implements RedisService {

    @Autowired
    private RedisTemplate jedisTemplate;

    @Override
    public boolean setNx(String key, String value, long expireTime, TimeUnit timeUnit) {
        Boolean result = (Boolean) jedisTemplate.execute((RedisConnection connection)-> {
            RedisSerializer<String> serializer = jedisTemplate.getStringSerializer();
            byte[] keyByte  = serializer.serialize(key);
            byte[] valueByte = serializer.serialize(value);
            connection.set(keyByte, valueByte, Expiration.from(expireTime, timeUnit), RedisStringCommands.SetOption.ifAbsent());
            connection.close();
            return true;
        });
        return result;
    }

    @Override
    public void set(String key, String value, long expireTime, TimeUnit timeUnit) {
        jedisTemplate.opsForValue().set(key, value, expireTime, timeUnit);
    }

    @Override
    public boolean del(String key) {
        Boolean result = (Boolean) jedisTemplate.execute((RedisConnection connection)-> {
            RedisSerializer<String> serializer = jedisTemplate.getStringSerializer();
            byte[] keyByte  = serializer.serialize(key);
            connection.del(keyByte);
            connection.close();
            return true;
        });
        return result;
    }

    @Override
    public Long incr(String key) {
        Long result = (Long) jedisTemplate.execute((RedisConnection connection)-> {
            RedisSerializer<String> serializer = jedisTemplate.getStringSerializer();
            byte[] keyByte  = serializer.serialize(key);
            Long incr = connection.incr(keyByte);
            connection.close();
            return incr;
        });
        return result;
    }

    @Override
    public Long decr(String key) {
        Long result = (Long) jedisTemplate.execute((RedisConnection connection)-> {
            RedisSerializer<String> serializer = jedisTemplate.getStringSerializer();
            byte[] keyByte  = serializer.serialize(key);
            Long decr = connection.decr(keyByte);
            connection.close();
            return decr;
        });
        return result;
    }

    @Override
    public Long ttl(String key) {
        Long result = (Long) jedisTemplate.execute((RedisConnection connection)-> {
            RedisSerializer<String> serializer = jedisTemplate.getStringSerializer();
            byte[] keyByte  = serializer.serialize(key);
            Long ttl = connection.ttl(keyByte);
            connection.close();
            return ttl;
        });
        return result;
    }

    @Override
    public Boolean pExpire(String key, long timeout) {
        Boolean result = (Boolean) jedisTemplate.execute((RedisConnection connection)-> {
            RedisSerializer<String> serializer = jedisTemplate.getStringSerializer();
            byte[] keyByte  = serializer.serialize(key);
            Boolean pExpire = connection.pExpire(keyByte, timeout);
            connection.close();
            return pExpire;
        });
        return result;
    }

    @Override
    public long countIncr(String key){
        long count;
        // 如果计数器不存在或者没有过期时间则计数++并且设置过期时间
        if(ttl(key) < 0){
            count = incr(key);
            pExpire(key, 1000 * 60 * 60);
        } else {
            count = incr(key);
        }
        return count;
    }

    @Override
    public long countDecr(String key){
        long ttl = ttl(key);
        if(ttl == -2){
            return ttl;
        } else if( ttl == -1){
            pExpire(key, 1000 * 60 * 60);
        }
        return decr(key);
    }
}
