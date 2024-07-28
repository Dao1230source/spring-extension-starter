package org.source.spring.cache;

import com.fasterxml.jackson.databind.JavaType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.source.spring.cache.configure.ConfigureCache;
import org.source.spring.cache.configure.ConfigureCacheProperties;
import org.source.spring.cache.configure.ConfigureTtlProperties;
import org.source.spring.cache.configure.ReturnTypeEnum;
import org.source.spring.cache.exception.CacheExceptionEnum;
import org.source.spring.cache.pubsub.ConfigureCacheMessageDelegate;
import org.source.spring.cache.strategy.PartialCacheStrategyEnum;
import org.source.spring.expression.SpElUtil;
import org.source.spring.redis.pubsub.MessageDelegate;
import org.source.spring.scan.ScanConfig;
import org.source.spring.scan.ScanProcessor;
import org.source.utility.constant.Constants;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Streams;
import org.source.utility.utils.Strings;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.cache.ConfigureRedisCacheWriter;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zengfugen
 */
@ConditionalOnProperty(prefix = "org.source.spring", name = "cache", matchIfMissing = true)
@Slf4j
@Data
@EnableCaching
@AutoConfigureAfter({ScanConfig.class})
@AutoConfiguration
public class ConfigureCacheConfig implements BeanFactoryAware {
    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(@NotNull BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    /**
     * spring cache 使用自定义的 CustomRedisCacheManager
     *
     * @param factory factory
     * @return RedisCacheManager bean
     */
    @Bean
    @ConditionalOnBean(ScanConfig.class)
    public RedisCacheManager cacheManager(RedisConnectionFactory factory,
                                          ScanConfig scanConfig,
                                          ScanProcessor redisCacheScanProcessor) {
        ConfigureRedisCacheWriter cacheWriter = new ConfigureRedisCacheWriter(factory);
        RedisCacheConfiguration defaultCacheConfiguration = defaultCacheConfig();
        // 在gateway使用时会出现 ScanConfig bean 已经注入成功但配置还未扫描出来的情况，因此这里手动执行
        scanConfig.doScan();
        List<ConfigureCacheProperties> allCacheConfigs = ((ScanProcessorConfig.RedisCacheScanProcessor) redisCacheScanProcessor).getConfigureCachePropertiesList();
        if (Objects.isNull(allCacheConfigs)) {
            log.warn("no @ConfigureCache annotated method");
            return new RedisCacheManager(cacheWriter, defaultCacheConfiguration);
        }
        if (log.isDebugEnabled()) {
            allCacheConfigs.forEach(k -> log.debug(Jsons.str(k)));
        }
        // 针对不同cacheName，设置不同的过期时间
        Map<String, RedisCacheConfiguration> configMap = configMap(allCacheConfigs);
        // 配置数据不可变
        Map<String, ConfigureCacheProperties> configureCacheExpendMap = Map.copyOf(Streams.toMap(allCacheConfigs, ConfigureCacheProperties::getCacheName));
        return new ConfigureRedisCacheManager(cacheWriter, defaultCacheConfiguration, configMap, true,
                configureCacheExpendMap);
    }

    @Bean
    public MessageDelegate configureCacheMessageDelegate(RedisCacheManager cacheManager) {
        if (cacheManager instanceof ConfigureRedisCacheManager configureRedisCacheManager) {
            return new ConfigureCacheMessageDelegate(configureRedisCacheManager.getConfigureCacheExpendMap());
        } else {
            return new ConfigureCacheMessageDelegate();
        }
    }

    public Map<String, RedisCacheConfiguration> configMap(List<ConfigureCacheProperties> allCacheConfigs) {
        // 优先注解配置，后配置的覆盖
        return Streams.of(allCacheConfigs)
                .collect(Collectors.toMap(ConfigureCacheProperties::getCacheName, this::getCacheConfigByExtend, (v1, v2) -> v2));
    }

    /**
     * 优先class上的注解，再找方法上的注解
     *
     * @param beanClass bean class
     * @return List<CacheConfig>
     */
    public static List<ConfigureCacheProperties> getCacheConfigFromBean(Class<?> beanClass, ConfigureTtlProperties configureTtlProperties) {
        return Streams.of(beanClass.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(ConfigureCache.class))
                .map(m -> {
                    List<ConfigureCacheProperties> configureCacheProperties =
                            ConfigureCacheProperties.convert(m.getAnnotation(ConfigureCache.class), configureTtlProperties);
                    configureCacheProperties.forEach(c -> assignValueClassesAndReturnType(m, c));
                    return configureCacheProperties;
                })
                .flatMap(Collection::stream)
                .toList();
    }

    protected static void assignValueClassesAndReturnType(Method method, ConfigureCacheProperties cacheProperties) {
        ReturnTypeEnum returnType = cacheProperties.getReturnType();
        if (ReturnTypeEnum.RAW.equals(returnType)) {
            cacheProperties.setReturnType(returnType);
            if (cacheProperties.isCacheInRedis()) {
                BaseExceptionEnum.NOT_NULL.nonNull(cacheProperties.getValueType(),
                        Strings.format("缓存到redis时returnType指定为RAW，valueClasses必不能为空, name:{}", cacheProperties.getCacheName()));
                cacheProperties.setValueType(cacheProperties.getValueType());
            }
            return;
        }
        PartialCacheStrategyEnum partialCache = cacheProperties.getPartialCache();
        if (PartialCacheStrategyEnum.PARTIAL_TRUST.equals(partialCache)) {
            String errorMessage = strategyCanSetBePartialTrust(cacheProperties.getKey(), method);
            CacheExceptionEnum.CANNOT_SET_STRATEGY_AS_PARTIAL_TRUST.isEmpty(errorMessage,
                    errorMessage + ", name:{}", cacheProperties.getCacheName());
        }
        Type methodReturnType = method.getGenericReturnType();
        // 当value的参数化类型只有一层时，可自动获取，
        if (methodReturnType instanceof ParameterizedType parameterizedType) {
            Class<?> rawType = (Class<?>) parameterizedType.getRawType();
            if (Map.class.isAssignableFrom(rawType)) {
                returnType = ReturnTypeEnum.MAP;
                methodReturnType = parameterizedType.getActualTypeArguments()[1];
            } else if (List.class.isAssignableFrom(rawType)) {
                returnType = ReturnTypeEnum.LIST;
                methodReturnType = parameterizedType.getActualTypeArguments()[0];
            } else if (Set.class.isAssignableFrom(rawType)) {
                returnType = ReturnTypeEnum.SET;
                methodReturnType = parameterizedType.getActualTypeArguments()[0];
            }
        }
        // 泛型时需要手动指定
        JavaType valueType = Jsons.getJavaType(methodReturnType);
        if (Objects.nonNull(cacheProperties.getValueType())) {
            valueType = cacheProperties.getValueType();
        }
        cacheProperties.setValueType(valueType);
        cacheProperties.setReturnType(returnType);
    }

    /**
     * <pre>
     * PARTIAL_TRUST 策略需要修改方法入参的值，为了简单默认方法只有一个参数，
     * 且{@literal ConfigureCache.key()}只能指向参数原始引用（key无值时默认是方法的第一个参数），
     * 只要key有任何计算，{@literal ConfigureRedisCache.get(Object key)}的key和方法入参都无法关联
     *
     * </pre>
     *
     * @param key    key
     * @param method method
     * @return boolean
     */
    protected static String strategyCanSetBePartialTrust(String key, Method method) {
        Parameter[] parameters = method.getParameters();
        if (parameters.length != 1) {
            return "方法有且只能有一个入参";
        }
        Parameter parameter = parameters[0];
        Class<?> type = parameter.getType();
        if (!Collection.class.isAssignableFrom(type)) {
            return "方法入参必须是Collection类型";
        }
        if (StringUtils.hasText(key)) {
            if (key.lastIndexOf(Constants.HASH) != 0) {
                return "key必须是spring expression，并且只有一个变量";
            }
            String k = key.substring(1);
            Set<String> parameterNames = Set.copyOf(Arrays.asList(SpElUtil.getParameterNames(method)));
            if (!parameterNames.contains(k)) {
                return "key的变量必须是方法的入参";
            }
        }
        return null;
    }

    /**
     * <pre>
     * 使用redis集群时，key通常分布在不同的节点上，如果批量获取key即{@literal MGET}命令，可能会报错
     * {@code io.lettuce.core.RedisCommandExecutionException: CROSSSLOT Keys in request don't hash to the same slot}
     * 为避免该错误，需要将同一个cacheName的key分配到同一个节点上，
     * 即使用redis的hash slot功能。
     * redis key像这样：{k}ey，其中 k 相同的key会分配到同一个节点
     * <br/>
     * cacheName 包装成 {cacheName}，这样生成的redis key就变成 {cacheName}::key
     * </pre>
     *
     * @param cacheProperties cacheProperties
     * @return RedisCacheConfiguration
     */
    public RedisCacheConfiguration getCacheConfigByExtend(ConfigureCacheProperties cacheProperties) {
        JavaType javaType = Jsons.getJavaType(cacheProperties.getValueType());
        Jackson2JsonRedisSerializer<Object> valueSerializer = getValueSerializer(javaType);
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(cacheProperties.getRedisTtl()))
                .disableCachingNullValues()
                .computePrefixWith(name -> Constants.LEFT_BRACE + name + Constants.RIGHT_BRACE + CacheKeyPrefix.SEPARATOR)
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer));
    }

    /**
     * 自定义 RedisCacheConfiguration 配置 目前只支持ttl配置
     */
    public RedisCacheConfiguration defaultCacheConfig() {
        JavaType javaType = Jsons.getJavaType(Object.class);
        Jackson2JsonRedisSerializer<Object> valueSerializer = getValueSerializer(javaType);
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(60))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer));
    }

    private Jackson2JsonRedisSerializer<Object> getValueSerializer(JavaType javaType) {
        return new Jackson2JsonRedisSerializer<>(Jsons.getInstance(), javaType);
    }

}