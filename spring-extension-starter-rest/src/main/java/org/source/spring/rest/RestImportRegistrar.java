package org.source.spring.rest;

import lombok.extern.slf4j.Slf4j;
import org.source.spring.common.AbstractContainerReadyImportRegistrar;
import org.source.spring.common.utility.EnableAnnotationUtil;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

@Slf4j
public class RestImportRegistrar extends AbstractContainerReadyImportRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry,
                                        ContextRefreshedEvent event) {
        log.debug("Register bean definitions for @EnableRest");
        ApplicationContext applicationContext = event.getApplicationContext();
        // 创建自定义扫描器，并关闭默认过滤器（这样只会扫描我们指定的注解）
        RestInterfaceScanner scanner = new RestInterfaceScanner(registry, applicationContext);
        // 添加包含过滤器，只扫描带有注解的类
        scanner.addIncludeFilter(new AnnotationTypeFilter(Rest.class));
        // 获取在@EnableMyServiceScan中指定的包路径，若未指定则使用启动类所在包
        String[] basePackages = EnableAnnotationUtil.getBasePackages(importingClassMetadata, EnableExtendedRest.class.getName());
        // 执行扫描
        scanner.scan(basePackages);
    }
}