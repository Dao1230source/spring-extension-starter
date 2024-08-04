package org.source.spring.log;

import org.source.spring.log.datasource.DataSourceProxyBeanPostProcessor;
import org.source.spring.log.handler.ControllerScopeHandler;
import org.source.spring.log.handler.DataSourceLogHandler;
import org.source.spring.log.handler.LogContextHandler;
import org.source.spring.log.handler.LogHandler;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@ConditionalOnProperty(prefix = "org.source.spring.enabled", name = "log", havingValue = "true", matchIfMissing = true)
@AutoConfiguration
public class LogConfig {

    @ConditionalOnMissingBean(LogDataProcessor.class)
    @Bean
    public LogDataProcessor defaultLogDataProcessor() {
        return new DefaultLogDataProcessor();
    }

    @ConditionalOnBean(LogDataProcessor.class)
    @Bean
    public Logs logs(LogDataProcessor logDataProcessor) {
        Logs logs = new Logs();
        Logs.setLogDataProcessor(logDataProcessor);
        return logs;
    }

    @Bean
    public StaticMethodMatcherPointcutAdvisor controllerScopeMethodAdvisor(ControllerScopeHandler processor) {
        return processor.createMethodAdviser();
    }

    @Bean
    public StaticMethodMatcherPointcutAdvisor logScopeMethodAdvisor(LogContextHandler processor) {
        return processor.createMethodAdviser();
    }

    @Bean
    public StaticMethodMatcherPointcutAdvisor logMethodAdvisor(LogHandler processor) {
        return processor.createMethodAdviser();
    }

    @Bean
    public StaticMethodMatcherPointcutAdvisor dataSourceMethodAdvisor(DataSourceLogHandler processor) {
        return processor.createMethodAdviser();
    }

    @Bean
    public DataSourceProxyBeanPostProcessor dataSourceProxyProcessor() {
        return new DataSourceProxyBeanPostProcessor();
    }

}
