package org.source.spring.cache;

import lombok.extern.slf4j.Slf4j;
import org.source.spring.cache.configure.ConfigureCache;
import org.source.spring.cache.configure.ConfigureCacheProperties;
import org.source.spring.cache.configure.ConfigureCachePropertiesCached;
import org.source.spring.common.AbstractImportRegistrar;
import org.source.spring.common.utility.ImportRegistrarUtil;
import org.source.utility.utils.Streams;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Slf4j
public class CacheImportRegistrar extends AbstractImportRegistrar {
    public static final String CONFIGURE_TTL_PREFIX = "org.source.spring.cache.";

    public CacheImportRegistrar() {
        super("cache");
    }

    @Override
    protected void registerBeanDefinitionsExtend(AnnotationMetadata importingClassMetadata,
                                                 BeanDefinitionRegistry registry) {
        log.debug("ImportBeanDefinitionRegistrar for @EnableExtendedCache");
        List<ConfigureCacheProperties> properties = this.obtainCacheProperties(importingClassMetadata);
        ImportRegistrarUtil.registerBeanDefinition(registry, ConfigureCachePropertiesCached.class, () -> new ConfigureCachePropertiesCached(properties));
    }

    private List<ConfigureCacheProperties> obtainCacheProperties(AnnotationMetadata importingClassMetadata) {
        // 创建自定义扫描器，并关闭默认过滤器（这样只会扫描我们指定的注解）
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        // 添加包含所有类的过滤器
        scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> true);
        // 添加包含过滤器，只扫描带有注解的类
        String[] basePackages = ImportRegistrarUtil.getBasePackages(importingClassMetadata, EnableExtendedCache.class.getName());
        ConfigureTtlProperties configureTtlProperties = this.obtainConfigureTtlProperties();
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

    protected ConfigureTtlProperties obtainConfigureTtlProperties() {
        Long redisTtl = environment.getProperty(CONFIGURE_TTL_PREFIX + "redis-ttl", Long.class);
        if (Objects.isNull(redisTtl)) {
            redisTtl = 300L;
        }
        Long jvmTtl = environment.getProperty(CONFIGURE_TTL_PREFIX + "jvm-ttl", Long.class);
        if (Objects.isNull(jvmTtl)) {
            jvmTtl = 300L;
        }
        return new ConfigureTtlProperties(redisTtl, jvmTtl);
    }


}