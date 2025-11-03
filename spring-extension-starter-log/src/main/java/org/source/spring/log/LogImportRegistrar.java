package org.source.spring.log;

import lombok.extern.slf4j.Slf4j;
import org.source.spring.common.AbstractImportRegistrar;
import org.source.spring.common.utility.ImportRegistrarUtil;
import org.source.spring.log.datasource.DataSourceProxyBeanPostProcessor;
import org.source.spring.log.processor.ControllerLogAnnoProcessor;
import org.source.spring.log.processor.DataSourceLogAnnoProcessor;
import org.source.spring.log.processor.LogContextLogAnnoProcessor;
import org.source.spring.log.processor.LogLogAnnoProcessor;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.core.type.AnnotationMetadata;

@Slf4j
public class LogImportRegistrar extends AbstractImportRegistrar {
    public LogImportRegistrar() {
        super(true);
    }

    @Override
    public void registerBeanDefinitionsExtend(AnnotationMetadata importingClassMetadata,
                                              BeanDefinitionRegistry registry) {
        ImportRegistrarUtil.registerBeanDefinitionPrototype(registry, StaticMethodMatcherPointcutAdvisor.class,
                "controllerScopeMethodAdvisor", () -> LogMethodAdviser.of(new ControllerLogAnnoProcessor()));
        ImportRegistrarUtil.registerBeanDefinitionPrototype(registry, StaticMethodMatcherPointcutAdvisor.class,
                "logContextMethodAdvisor", () -> LogMethodAdviser.of(new LogContextLogAnnoProcessor()));
        ImportRegistrarUtil.registerBeanDefinitionPrototype(registry, StaticMethodMatcherPointcutAdvisor.class,
                "logMethodAdvisor", () -> LogMethodAdviser.of(new LogLogAnnoProcessor()));
        ImportRegistrarUtil.registerBeanDefinitionPrototype(registry, StaticMethodMatcherPointcutAdvisor.class,
                "dataSourceMethodAdvisor", () -> LogMethodAdviser.of(new DataSourceLogAnnoProcessor()));
        ImportRegistrarUtil.registerBeanDefinition(registry, DataSourceProxyBeanPostProcessor.class,
                DataSourceProxyBeanPostProcessor::new);
    }

    @Override
    protected void registerBeanDefinitionsAfterContainerReady(AnnotationMetadata importingClassMetadata,
                                                              BeanDefinitionRegistry registry,
                                                              ApplicationContext applicationContext) {
        try {
            LogDataProcessor logDataProcessor = applicationContext.getBean(LogDataProcessor.class);
            Logs.setLogDataProcessor(logDataProcessor);
        } catch (Exception e) {
            log.warn("LogDataProcessor not found, use DefaultLogDataProcessor instead.");
            Logs.setLogDataProcessor(new DefaultLogDataProcessor());
        }
    }
}