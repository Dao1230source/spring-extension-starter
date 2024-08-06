package org.source.spring.i18n;

import jakarta.validation.MessageInterpolator;
import org.hibernate.validator.resourceloading.CachingResourceBundleLocator;
import org.source.spring.i18n.validate.DictMessageSource;
import org.source.spring.i18n.validate.ValidMessageInterpolator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MessageSourceResourceBundleLocator;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

@ConditionalOnProperty(prefix = "org.source.spring.enabled", name = "i18n", matchIfMissing = true)
@AutoConfiguration
public class I18nBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof LocalValidatorFactoryBean localValidatorFactoryBean) {
            localValidatorFactoryBean.setMessageInterpolator(getMessageInterpolator(getMessageSource()));
        }
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

    @Bean
    public MessageSource getMessageSource() {
        return new DictMessageSource();
    }

    @Bean
    public MessageInterpolator getMessageInterpolator(MessageSource messageSource) {
        MessageSourceResourceBundleLocator resourceBundleLocator = new MessageSourceResourceBundleLocator(messageSource);
        CachingResourceBundleLocator cachingResourceBundleLocator = new CachingResourceBundleLocator(resourceBundleLocator);
        return new ValidMessageInterpolator(cachingResourceBundleLocator);
    }
}
