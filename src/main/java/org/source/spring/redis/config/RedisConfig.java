package org.source.spring.redis.config;

import org.source.utility.utils.Jsons;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @author zengfugen
 */
@ConditionalOnProperty(prefix = "org.source.spring", name = "redis", matchIfMissing = true)
@AutoConfiguration
public class RedisConfig {

    @Bean
    @ConditionalOnMissingBean
    public StringRedisTemplate redisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(factory);
        RedisSerializer<String> keySerializer = new StringRedisSerializer();
        redisTemplate.setKeySerializer(keySerializer);
        redisTemplate.setHashKeySerializer(keySerializer);
        RedisSerializer<String> valueSerializer = new Jackson2JsonRedisSerializer<>(Jsons.getInstance(), String.class);
        redisTemplate.setValueSerializer(valueSerializer);
        redisTemplate.setHashValueSerializer(valueSerializer);
        // 使设置生效
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

}
