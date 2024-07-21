package org.source.spring.cache;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.source.spring.cache.configure.ConfigureCacheProperties;
import org.springframework.data.redis.cache.ConfigureRedisCacheWriter;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * @author zengfugen
 */
@Getter
public class ConfigureRedisCacheManager extends RedisCacheManager {
    private final ConfigureRedisCacheWriter cacheWriter;
    private final RedisCacheConfiguration defaultCacheConfig;
    private final Map<String, RedisCacheConfiguration> initialCacheConfiguration;
    private final boolean allowInFlightCacheCreation;
    private final Map<String, ConfigureCacheProperties> configureCacheExpendMap;

    public ConfigureRedisCacheManager(ConfigureRedisCacheWriter cacheWriter,
                                      RedisCacheConfiguration defaultCacheConfiguration,
                                      Map<String, RedisCacheConfiguration> initialCacheConfigurations,
                                      boolean allowInFlightCacheCreation,
                                      @NotNull Map<String, ConfigureCacheProperties> configureCacheExpendMap) {
        super(cacheWriter, defaultCacheConfiguration, initialCacheConfigurations, allowInFlightCacheCreation);
        this.cacheWriter = cacheWriter;
        this.defaultCacheConfig = defaultCacheConfiguration;
        this.initialCacheConfiguration = initialCacheConfigurations;
        this.allowInFlightCacheCreation = allowInFlightCacheCreation;
        this.configureCacheExpendMap = configureCacheExpendMap;
        this.setTransactionAware(true);
    }

    @Override
    protected @NotNull RedisCache createRedisCache(@NonNull String name, RedisCacheConfiguration cacheConfig) {
        return new ConfigureRedisCache(name, cacheWriter, cacheConfig != null ? cacheConfig : defaultCacheConfig,
                configureCacheExpendMap);
    }
}
