package com.ducseul.apiforwarder.service;

import com.ducseul.apiforwarder.entity.ResponseCacheEntity;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisService {
    private final RedisTemplate<String, ResponseCacheEntity> redisTemplate;

    public RedisService(RedisTemplate<String, ResponseCacheEntity> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void putValues(String key, ResponseCacheEntity value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public ResponseCacheEntity getValues(String key) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return redisTemplate.opsForValue().get(key);
        } else {
            return null;
        }
    }

}
