package org.source.spring.cache;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.source.spring.cache.configure.ConfigureCache;
import org.source.spring.cache.configure.ConfigureCacheProperties;
import org.source.spring.cache.configure.ConfigureTtlProperties;
import org.source.spring.scan.AbstractAnnotationScanProcessor;
import org.source.spring.scan.ScanConfig;
import org.source.spring.scan.ScanProcessor;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

@AutoConfigureBefore(ScanConfig.class)
@AutoConfiguration
public class ScanProcessorConfig implements BeanFactoryAware {
    private BeanFactory beanFactory;
    private final ConfigureTtlProperties configureTtlProperties;

    public ScanProcessorConfig(ConfigureTtlProperties configureTtlProperties) {
        this.configureTtlProperties = configureTtlProperties;
    }

    @Override
    public void setBeanFactory(@NotNull BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @EqualsAndHashCode(callSuper = false)
    protected class RedisCacheScanProcessor extends AbstractAnnotationScanProcessor {
        @Getter
        private List<ConfigureCacheProperties> configureCachePropertiesList;
        private final BeanFactory beanFactory;

        public RedisCacheScanProcessor(List<Class<? extends Annotation>> annotationTypeList, BeanFactory beanFactory) {
            super(annotationTypeList);
            this.beanFactory = beanFactory;
        }

        @Override
        public void processClasses(@NotNull List<Class<?>> classes) {
            this.configureCachePropertiesList = classes.stream().map(this.beanFactory::getBean).map(AopUtils::getTargetClass)
                    .map(c -> ConfigureCacheConfig.getCacheConfigFromBean(c, configureTtlProperties))
                    .flatMap(Collection::stream).toList();
        }
    }

    @Bean
    public ScanProcessor redisCacheScanProcessor() {
        return new RedisCacheScanProcessor(List.of(ConfigureCache.class), this.beanFactory);
    }

}
