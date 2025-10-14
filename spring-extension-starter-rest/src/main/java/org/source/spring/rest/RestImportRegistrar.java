package org.source.spring.rest;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.source.spring.common.utility.EnableAnnotationUtil;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

@Slf4j
public class RestImportRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {
    private Environment environment;

    @Override
    public void setEnvironment(@NotNull Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(@NotNull AnnotationMetadata importingClassMetadata,
                                        @NotNull BeanDefinitionRegistry registry,
                                        @NotNull BeanNameGenerator importBeanNameGenerator) {
        log.debug("Register bean definitions for @EnableRest");
        // 创建自定义扫描器，并关闭默认过滤器（这样只会扫描我们指定的注解）
        RestInterfaceScanner scanner = new RestInterfaceScanner(registry, this.environment);
        // 添加包含过滤器，只扫描带有注解的类
        scanner.addIncludeFilter(new AnnotationTypeFilter(Rest.class));
        // 获取在@EnableMyServiceScan中指定的包路径，若未指定则使用启动类所在包
        String[] basePackages = EnableAnnotationUtil.getBasePackages(importingClassMetadata, EnableExtendedRest.class.getName());
        // 执行扫描
        scanner.scan(basePackages);
    }

}
