package org.source.spring.redis;

import org.source.spring.redis.config.RedisConfig;
import org.source.spring.redis.pubsub.RedisPubsubConfig;
import org.source.spring.redis.redisson.RedissonConfig;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
// 本配置类必须在 RedisAutoConfiguration 之后配置
@AutoConfigureAfter(RedisAutoConfiguration.class)
@Import({RedisConfig.class, RedisPubsubConfig.class, RedissonConfig.class})
public @interface EnableExtendedRedis {
}
