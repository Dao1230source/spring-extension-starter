package org.source.spring.cache;

import lombok.extern.slf4j.Slf4j;
import org.source.spring.cache.configure.ConfigureCacheProperties;
import org.source.spring.cache.pubsub.ConfigureCacheMessageDelegate;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Streams;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.ConfigureRedisCacheWriter;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@AutoConfiguration
public class RedisConfig {

    @Primary
    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory,
                                               ConfigureCachePropertiesCached configureCachePropertiesCached) {
        return this.cacheManager(redisConnectionFactory, configureCachePropertiesCached.getConfigureCacheProperties());
    }

    @Primary
    @Bean
    public ConfigureCacheMessageDelegate getConfigureCacheMessageDelegate(RedisCacheManager redisCacheManager) {
        if (redisCacheManager instanceof ConfigureRedisCacheManager configureRedisCacheManager) {
            return new ConfigureCacheMessageDelegate(configureRedisCacheManager.getConfigureCacheExpendMap());
        } else {
            return new ConfigureCacheMessageDelegate();
        }
    }

    public RedisCacheManager cacheManager(RedisConnectionFactory factory, List<ConfigureCacheProperties> allCacheConfigs) {
        ConfigureRedisCacheWriter cacheWriter = new ConfigureRedisCacheWriter(factory);
        RedisCacheConfiguration defaultCacheConfiguration = ConfigureCacheUtil.defaultCacheConfig();
        if (CollectionUtils.isEmpty(allCacheConfigs)) {
            log.warn("no @ConfigureCache annotated method");
            return new RedisCacheManager(cacheWriter, defaultCacheConfiguration);
        }
        if (log.isDebugEnabled()) {
            allCacheConfigs.forEach(k -> log.debug(Jsons.str(k)));
        }
        // 针对不同cacheName，设置不同的过期时间
        Map<String, RedisCacheConfiguration> configMap = ConfigureCacheUtil.configMap(allCacheConfigs);
        // 配置数据不可变
        Map<String, ConfigureCacheProperties> configureCacheExpendMap = Map.copyOf(Streams.toMap(allCacheConfigs, ConfigureCacheProperties::getCacheName));
        return new ConfigureRedisCacheManager(cacheWriter, defaultCacheConfiguration, configMap, true,
                configureCacheExpendMap);
    }
}
