package org.source.spring.utility;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.NonNull;

import java.util.Map;

/**
 * @author zengfugen
 */
@Slf4j
@ConditionalOnProperty(prefix = "org.source.spring", name = "utility", matchIfMissing = true)
@AutoConfiguration
public class SpringUtil implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public synchronized void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        SpringUtil.context = applicationContext;
    }

    public static synchronized ApplicationContext getApplicationContext() {
        return context;
    }

    public static <T> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }

    public static <T> Map<String, T> getBeansOfType(Class<T> tClass) {
        return context.getBeansOfType(tClass);
    }

    public static void registerBean(Object bean) {
        ConfigurableListableBeanFactory beanFactory = ((ConfigurableApplicationContext) context).getBeanFactory();
        beanFactory.registerSingleton(bean.getClass().getSimpleName(), bean);
    }

    public static String getContextPath() {
        return context.getApplicationName();
    }
}
