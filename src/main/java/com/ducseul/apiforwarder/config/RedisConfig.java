package com.ducseul.apiforwarder.config;

import com.ducseul.apiforwarder.entity.ResponseCacheEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
//@ConditionalOnProperty(name = "application.redis.enabled", havingValue = "true")
public class RedisConfig {
    @Bean
    public RedisTemplate<String, ResponseCacheEntity> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, ResponseCacheEntity> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
