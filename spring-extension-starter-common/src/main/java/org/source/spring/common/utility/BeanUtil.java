package org.source.spring.common.utility;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

@UtilityClass
@Slf4j
public class BeanUtil {

    public static void registerBean(BeanFactory beanFactory, String beanName, Object bean) {
        SingletonBeanRegistry beanRegistry = (SingletonBeanRegistry) beanFactory;
        beanRegistry.registerSingleton(beanName, bean);
    }

    public AbstractBeanDefinition createBeanDefinition(Class<?> beanClass) {
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(beanClass);
        AbstractBeanDefinition beanDefinition = beanDefinitionBuilder.getBeanDefinition();
        // 设置优先依赖
        beanDefinition.setPrimary(true);
        beanDefinition.setBeanClass(beanClass);
        beanDefinition.setLazyInit(true);
        beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        return beanDefinition;
    }
}
