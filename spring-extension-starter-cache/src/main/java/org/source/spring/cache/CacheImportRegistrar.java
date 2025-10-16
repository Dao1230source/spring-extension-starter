package org.source.spring.cache;

import lombok.extern.slf4j.Slf4j;
import org.source.spring.cache.configure.ConfigureCache;
import org.source.spring.cache.configure.ConfigureCacheProperties;
import org.source.spring.cache.pubsub.ConfigureCacheMessageDelegate;
import org.source.spring.common.AbstractContainerReadyImportRegistrar;
import org.source.spring.common.utility.ImportRegistrarUtil;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Streams;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.redis.cache.ConfigureRedisCacheWriter;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
public class CacheImportRegistrar extends AbstractContainerReadyImportRegistrar {

    @Override
    protected void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                           BeanDefinitionRegistry registry,
                                           ApplicationContext applicationContext) {
        log.debug("ImportBeanDefinitionRegistrar for @EnableExtendedCache");
        List<ConfigureCacheProperties> properties = this.obtainCacheProperties(importingClassMetadata, this.resourceLoader, applicationContext);
        RedisConnectionFactory redisConnectionFactory = applicationContext.getBean(RedisConnectionFactory.class);
        RedisCacheManager redisCacheManager = this.cacheManager(redisConnectionFactory, properties);
        ImportRegistrarUtil.registerBeanDefinition(registry, RedisCacheManager.class, () -> redisCacheManager);
        ImportRegistrarUtil.registerBeanDefinition(registry, ConfigureCacheMessageDelegate.class, () -> {
            if (redisCacheManager instanceof ConfigureRedisCacheManager configureRedisCacheManager) {
                return new ConfigureCacheMessageDelegate(configureRedisCacheManager.getConfigureCacheExpendMap());
            } else {
                return new ConfigureCacheMessageDelegate();
            }
        });
    }

    private List<ConfigureCacheProperties> obtainCacheProperties(AnnotationMetadata importingClassMetadata,
                                                                 ResourceLoader resourceLoader,
                                                                 ApplicationContext applicationContext) {
        // 创建自定义扫描器，并关闭默认过滤器（这样只会扫描我们指定的注解）
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        // 添加包含所有类的过滤器
        scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> true);
        // 添加包含过滤器，只扫描带有注解的类
        String[] basePackages = ImportRegistrarUtil.getBasePackages(importingClassMetadata, EnableExtendedCache.class.getName());
        ConfigureTtlProperties configureTtlProperties = applicationContext.getBean(ConfigureTtlProperties.class);
        return Streams.of(basePackages)
                .map(basePackage -> Streams.of(scanner.findCandidateComponents(basePackage))
                        .map(beanDefinition -> {
                            if (StringUtils.hasLength(beanDefinition.getBeanClassName())) {
                                Class<?> clazz = ClassUtils.resolveClassName(beanDefinition.getBeanClassName(), this.resourceLoader.getClassLoader());
                                return Streams.of(ReflectionUtils.getDeclaredMethods(clazz))
                                        .filter(method -> method.isAnnotationPresent(ConfigureCache.class))
                                        .toList();
                            } else {
                                return List.<Method>of();
                            }
                        }).flatMap(Collection::stream).toList())
                .flatMap(Collection::stream)
                .map(m -> {
                    List<ConfigureCacheProperties> configureCacheProperties =
                            ConfigureCacheProperties.convert(m.getAnnotation(ConfigureCache.class), configureTtlProperties);
                    configureCacheProperties.forEach(c -> ConfigureCacheUtil.assignValueClassesAndReturnType(m, c));
                    return configureCacheProperties;
                }).flatMap(Collection::stream).toList();
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