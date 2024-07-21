package org.source.spring.cache.configure;

import com.fasterxml.jackson.databind.JavaType;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.source.spring.cache.strategy.PartialCacheStrategyEnum;
import org.source.spring.cache.constant.CacheConstant;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Streams;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author zengfugen
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ConfigureCacheProperties {
    private String cacheName;
    private String key;
    /**
     * 过期时间，单位：秒
     */
    private long redisTtl;
    /**
     * 缓存值的类型jackson的JavaType
     */
    private JavaType valueType;

    /**
     * 当返回值是List/Set时，value生成redis key的spEl表达式
     */
    private String cacheKeySpEl;
    private ReturnTypeEnum returnType;
    private boolean cacheInRedis;
    private PartialCacheStrategyEnum partialCache;

    private boolean cacheInJvm;
    private long jvmTtl;
    private long jvmMaxSize = 10000L;
    private Class<?> jvmKeyClass;
    private Cache<Object, Object> jvmCache;


    public static List<ConfigureCacheProperties> convert(ConfigureCache configureCache,
                                                         ConfigureTtlProperties configureTtlProperties) {
        String[] cacheNames = configureCache.cacheNames();
        return Streams.of(cacheNames).map(n -> {
            CacheInRedis redis = configureCache.cacheInRedis();
            JavaType javaType = Jsons.getJavaType(Object.class);
            if (redis.enable()) {
                javaType = Jsons.getJavaType(redis.valueClasses());
            }
            long redisTtl = configureCache.cacheInRedis().ttl();
            if (redisTtl == CacheConstant.FROM_CONFIG) {
                redisTtl = Objects.requireNonNullElse(configureTtlProperties.getRedisTtl(), CacheConstant.TTL_DEFAULT);
            }
            ConfigureCachePropertiesBuilder builder = ConfigureCacheProperties.builder();
            builder.cacheName(n).key(configureCache.key())
                    .redisTtl(redisTtl)
                    .cacheKeySpEl(configureCache.cacheKeySpEl())
                    .returnType(configureCache.returnType())
                    .cacheInRedis(redis.enable())
                    .partialCache(configureCache.partialCacheStrategy())
                    .valueType(javaType)
                    .build();
            CacheInJvm jvm = configureCache.cacheInJvm();
            if (jvm.enable()) {
                Caffeine<Object, Object> caffeineBuilder = Caffeine.newBuilder();
                if (jvm.jvmMaxSize() > 0) {
                    caffeineBuilder.maximumSize(jvm.jvmMaxSize());
                }
                long ttl = jvm.ttl();
                if (ttl == CacheConstant.FROM_CONFIG) {
                    ttl = Objects.requireNonNullElse(configureTtlProperties.getJvmTtl(), CacheConstant.TTL_DEFAULT);
                }
                if (ttl > 0) {
                    caffeineBuilder.expireAfterAccess(ttl, TimeUnit.SECONDS);
                }
                builder.cacheInJvm(true)
                        .jvmTtl(ttl)
                        .jvmMaxSize(jvm.jvmMaxSize())
                        .jvmKeyClass(jvm.keyClass())
                        .jvmCache(caffeineBuilder.build());
            }
            return builder.build();
        }).toList();
    }
}
