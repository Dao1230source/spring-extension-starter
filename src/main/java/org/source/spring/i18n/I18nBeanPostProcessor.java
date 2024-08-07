package org.source.spring.i18n;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

@ConditionalOnProperty(prefix = "org.source.spring.enabled", name = "i18n", matchIfMissing = true)
@AutoConfiguration
public class I18nBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof AcceptHeaderLocaleResolver localeResolver) {
            localeResolver.setDefaultLocale(Locale.getDefault());
            localeResolver.setSupportedLocales(List.of(Locale.TRADITIONAL_CHINESE, Locale.SIMPLIFIED_CHINESE, Locale.ENGLISH));
        }
        if (bean instanceof AcceptHeaderLocaleContextResolver localeContextResolver) {
            localeContextResolver.setDefaultLocale(Locale.getDefault());
            localeContextResolver.setSupportedLocales(List.of(Locale.TRADITIONAL_CHINESE, Locale.SIMPLIFIED_CHINESE, Locale.ENGLISH));
        }
        return bean;
    }

}
