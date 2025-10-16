package org.source.spring.cache;

import org.source.spring.cache.strategy.ConfigureCacheInterceptor;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.cache.annotation.ProxyCachingConfiguration;
import org.springframework.cache.interceptor.BeanFactoryCacheOperationSourceAdvisor;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.cache.interceptor.CacheOperationSource;

@AutoConfigureAfter({ProxyCachingConfiguration.class})
@AutoConfiguration
public class ConfigureInterceptorConfig implements BeanFactoryAware {

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        BeanFactoryCacheOperationSourceAdvisor bean = beanFactory.getBean(BeanFactoryCacheOperationSourceAdvisor.class);
        CacheOperationSource operationSource = beanFactory.getBean(CacheOperationSource.class);
        CacheInterceptor cacheInterceptor = beanFactory.getBean(CacheInterceptor.class);
        ConfigureCacheInterceptor interceptor = new ConfigureCacheInterceptor();
        interceptor.setBeanFactory(beanFactory);
        interceptor.setCacheOperationSource(operationSource);
        interceptor.afterSingletonsInstantiated();
        interceptor.configure(cacheInterceptor::getErrorHandler, cacheInterceptor::getKeyGenerator,
                cacheInterceptor::getCacheResolver, () -> beanFactory.getBean(ConfigureRedisCacheManager.class));
        bean.setAdvice(interceptor);
    }
}