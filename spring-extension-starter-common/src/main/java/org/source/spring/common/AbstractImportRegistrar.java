package org.source.spring.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import java.util.function.Consumer;

@Slf4j
public abstract class AbstractImportRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {
    /**
     * 如果名称不为空，则注册springboot 容器准备完成后的监听器
     */
    private final String containerReadyEventListenerName;

    protected ResourceLoader resourceLoader;
    protected Environment environment;

    protected AbstractImportRegistrar(String containerReadyEventListenerName) {
        this.containerReadyEventListenerName = containerReadyEventListenerName;
    }

    protected AbstractImportRegistrar() {
        this.containerReadyEventListenerName = "";
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        if (StringUtils.hasText(this.containerReadyEventListenerName)) {
            this.registerContainerReadyEventListener(importingClassMetadata, registry);
        }
        this.registerBeanDefinitionsExtend(importingClassMetadata, registry);
    }

    /**
     * 原 registerBeanDefinitions
     *
     * @param importingClassMetadata importingClassMetadata
     * @param registry               registry
     */
    protected void registerBeanDefinitionsExtend(AnnotationMetadata importingClassMetadata,
                                                 BeanDefinitionRegistry registry) {
    }

    protected void registerContainerReadyEventListener(AnnotationMetadata importingClassMetadata,
                                                       BeanDefinitionRegistry registry) {
        GenericBeanDefinition containerReadyBeanDefinition = new GenericBeanDefinition();
        containerReadyBeanDefinition.setBeanClass(ContainerReadyEventListener.class);
        // 多例
        containerReadyBeanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
        // 框架内部的基础设施组件
        containerReadyBeanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        Consumer<ApplicationContextEvent> consumer = event ->
                this.registerBeanDefinitionsAfterContainerReady(importingClassMetadata, registry, event.getApplicationContext());
        containerReadyBeanDefinition.setInstanceSupplier(() -> new ContainerReadyEventListener(consumer));
        registry.registerBeanDefinition(this.containerReadyEventListenerName + "_containerReadyEventListener", containerReadyBeanDefinition);
    }

    /**
     * springboot 容器启动完成后的 registerBeanDefinitions
     *
     * @param importingClassMetadata importingClassMetadata
     * @param registry               registry
     * @param applicationContext     applicationContext
     */
    protected void registerBeanDefinitionsAfterContainerReady(AnnotationMetadata importingClassMetadata,
                                                              BeanDefinitionRegistry registry,
                                                              ApplicationContext applicationContext) {
    }


    public static class ContainerReadyEventListener
            implements ApplicationListener<ApplicationContextEvent> {
        private final Consumer<ApplicationContextEvent> consumer;
        private boolean executed;

        public ContainerReadyEventListener(Consumer<ApplicationContextEvent> consumer) {
            this.consumer = consumer;
            this.executed = false;
        }

        @Override
        public synchronized void onApplicationEvent(ApplicationContextEvent event) {
            // 避免重复触发（例如在Web应用中）
            if (executed) {
                return;
            }
            // 在这里编写容器完全刷新后需要执行的代码
            // ApplicationContext 已经准备就绪，可以安全地获取任何Bean
            consumer.accept(event);
            this.executed = true;
        }
    }
}