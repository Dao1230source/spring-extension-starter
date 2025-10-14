package org.source.spring.cache;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.source.spring.cache.configure.ConfigureCache;
import org.source.spring.cache.configure.ConfigureCacheProperties;
import org.source.spring.cache.pubsub.ConfigureCacheMessageDelegate;
import org.source.spring.common.utility.EnableAnnotationUtil;
import org.source.spring.redis.pubsub.MessageDelegate;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Reflects;
import org.source.utility.utils.Streams;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.redis.cache.ConfigureRedisCacheWriter;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.util.ReflectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class CacheImportRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {
    public static final String PROPERTIES_PREFIX_CACHE = "org.source.spring.cache.";
    private ConfigureTtlProperties configureTtlProperties;

    @Override
    public void setEnvironment(@NotNull Environment environment) {
        long redisTtl = Long.parseLong(environment.getProperty(PROPERTIES_PREFIX_CACHE + "redisTtl", "300"));
        long jvmTtl = Long.parseLong(environment.getProperty(PROPERTIES_PREFIX_CACHE + "jvmTtl", "300"));
        this.configureTtlProperties = new ConfigureTtlProperties(redisTtl, jvmTtl);
    }

    @Override
    public void registerBeanDefinitions(@NotNull AnnotationMetadata importingClassMetadata,
                                        @NotNull BeanDefinitionRegistry registry) {
        log.debug("Register bean definitions for @EnableExtendedCache");
        // 创建自定义扫描器，并关闭默认过滤器（这样只会扫描我们指定的注解）
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        // 添加包含过滤器，只扫描带有注解的类
        scanner.addIncludeFilter(new AnnotationTypeFilter(ConfigureCache.class));
        String[] basePackages = EnableAnnotationUtil.getBasePackages(importingClassMetadata, EnableExtendedCache.class.getName());
        List<ConfigureCacheProperties> properties = this.obtainCacheProperties(scanner, basePackages);
        RedisConnectionFactory redisConnectionFactory = EnableAnnotationUtil.getBeanFromRegistry(registry, RedisConnectionFactory.class);
        RedisCacheManager redisCacheManager = this.cacheManager(redisConnectionFactory, properties);
        this.registerRedisCacheManager(registry, redisCacheManager);
        this.registerMessageDelegate(registry, redisCacheManager);
    }

    public void registerRedisCacheManager(BeanDefinitionRegistry registry, RedisCacheManager redisCacheManager) {
        // 注册 RedisCacheManager
        GenericBeanDefinition redisCacheManagerBeanDefinition = new GenericBeanDefinition();
        redisCacheManagerBeanDefinition.setBeanClass(RedisCacheManager.class);
        redisCacheManagerBeanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
        redisCacheManagerBeanDefinition.setInstanceSupplier(() -> redisCacheManager);
        registry.registerBeanDefinition("redisCacheManager", redisCacheManagerBeanDefinition);
    }

    public void registerMessageDelegate(BeanDefinitionRegistry registry, RedisCacheManager redisCacheManager) {
        // 注册 删除本地缓存的pubsub MessageDelegate
        GenericBeanDefinition messageDelegateBeanDefinition = new GenericBeanDefinition();
        messageDelegateBeanDefinition.setBeanClass(MessageDelegate.class);
        messageDelegateBeanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
        messageDelegateBeanDefinition.setInstanceSupplier(() -> {
            if (redisCacheManager instanceof ConfigureRedisCacheManager configureRedisCacheManager) {
                return new ConfigureCacheMessageDelegate(configureRedisCacheManager.getConfigureCacheExpendMap());
            } else {
                return new ConfigureCacheMessageDelegate();
            }
        });
        registry.registerBeanDefinition("messageDelegate", messageDelegateBeanDefinition);
    }

    private List<ConfigureCacheProperties> obtainCacheProperties(ClassPathScanningCandidateComponentProvider scanner, String[] basePackages) {
        return Streams.of(basePackages)
                .map(basePackage -> Streams.of(scanner.findCandidateComponents(basePackage))
                        .map(beanDefinition -> {
                            // 加载类对象
                            Class<?> clazz = Reflects.classForName(beanDefinition.getBeanClassName());
                            return Streams.of(ReflectionUtils.getDeclaredMethods(clazz))
                                    .filter(method -> method.isAnnotationPresent(EnableExtendedCache.class))
                                    .toList();
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
        if (Objects.isNull(allCacheConfigs)) {
            log.warn("no @ConfigureCache annotated method");
            return new RedisCacheManager(cacheWriter, defaultCacheConfiguration);
        }
        if (log.isDebugEnabled()) {
            allCacheConfigs.forEach(k -> log.debug(Jsons.str(k)));
        }
        // 针对不同cacheName，设置不同的过期时间
        Map<String, RedisCacheConfiguration> configMap = ConfigureCacheUtil.configMap(allCacheConfigs);
        log.info("create RedisCacheManager");
        // 配置数据不可变
        Map<String, ConfigureCacheProperties> configureCacheExpendMap = Map.copyOf(Streams.toMap(allCacheConfigs, ConfigureCacheProperties::getCacheName));
        return new ConfigureRedisCacheManager(cacheWriter, defaultCacheConfiguration, configMap, true,
                configureCacheExpendMap);
    }

}
