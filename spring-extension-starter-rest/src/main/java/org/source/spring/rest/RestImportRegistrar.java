package org.source.spring.rest;

import lombok.extern.slf4j.Slf4j;
import org.source.spring.common.AbstractImportRegistrar;
import org.source.spring.common.utility.ImportRegistrarUtil;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

@Slf4j
public class RestImportRegistrar extends AbstractImportRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        log.debug("ImportBeanDefinitionRegistrar for @EnableRest");
        // 创建自定义扫描器，并关闭默认过滤器（这样只会扫描我们指定的注解）
        RestInterfaceScanner scanner = new RestInterfaceScanner(registry, this.environment, this.resourceLoader);
        // 添加包含过滤器，只扫描带有注解的类
        scanner.addIncludeFilter(new AnnotationTypeFilter(Rest.class));
        // 获取在@EnableMyServiceScan中指定的包路径，若未指定则使用启动类所在包
        String[] basePackages = ImportRegistrarUtil.getBasePackages(importingClassMetadata, EnableExtendedRest.class.getName());
        // 执行扫描
        scanner.scan(basePackages);
    }
}