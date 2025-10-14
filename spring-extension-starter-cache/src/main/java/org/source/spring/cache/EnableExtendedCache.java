package org.source.spring.cache;

import org.source.spring.redis.EnableExtendedRedis;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnableCaching
@EnableExtendedRedis
@AutoConfigureAfter(RedisAutoConfiguration.class)
@Import({CacheImportRegistrar.class})
public @interface EnableExtendedCache {
    String[] basePackages() default {};
}
