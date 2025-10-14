package org.source.spring.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.type.AnnotationMetadata;

import java.util.function.Consumer;

@Slf4j
public abstract class AbstractContainerReadyImportRegistrar implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        GenericBeanDefinition containerReadyBeanDefinition = new GenericBeanDefinition();
        containerReadyBeanDefinition.setBeanClass(ContainerReadyEventListener.class);
        // 多例
        containerReadyBeanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
        // 框架内部的基础设施组件
        containerReadyBeanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        Consumer<ContextRefreshedEvent> consumer = event -> this.registerBeanDefinitions(importingClassMetadata, registry, event);
        containerReadyBeanDefinition.setInstanceSupplier(() -> new ContainerReadyEventListener(consumer));
        registry.registerBeanDefinition("containerReadyEventListener", containerReadyBeanDefinition);
    }

    protected abstract void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                                    BeanDefinitionRegistry registry,
                                                    ContextRefreshedEvent event);
}