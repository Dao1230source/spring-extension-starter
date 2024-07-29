package org.source.spring.i18n;

import jakarta.validation.MessageInterpolator;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.resourceloading.CachingResourceBundleLocator;
import org.jetbrains.annotations.NotNull;
import org.source.spring.cache.ConfigureCacheConfig;
import org.source.spring.i18n.facade.param.Dict4Param;
import org.source.spring.i18n.processor.DefaultProcessor;
import org.source.spring.i18n.processor.Processor;
import org.source.spring.i18n.validate.DictMessageSource;
import org.source.spring.i18n.validate.ValidMessageInterpolator;
import org.source.spring.io.ValidateConfig;
import org.source.spring.scan.ScanConfig;
import org.source.spring.utility.BeanUtil;
import org.source.utility.utils.Reflects;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MessageSourceResourceBundleLocator;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

@Slf4j
@ConditionalOnProperty(prefix = "org.source.spring.enabled", name = "i18n", matchIfMissing = true)
@AutoConfiguration(after = {ValidateConfig.class, ConfigureCacheConfig.class, ScanConfig.class})
public class I18nConfig implements InitializingBean, BeanFactoryAware {
    private static final String WEB_HTTP_CLASS = "jakarta.servlet.http.HttpServletRequest";
    private static final String LOCALE_RESOLVER_WEB_BEAN = "localeResolver";
    private static final String LOCALE_RESOLVER_WEB_FLUX_BEAN = "localeContextResolver";

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(@NotNull BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterPropertiesSet() {
        // 基于已有LocalValidatorFactoryBean配置
        LocalValidatorFactoryBean localValidatorFactoryBean = this.beanFactory.getBean(LocalValidatorFactoryBean.class);
        localValidatorFactoryBean.setMessageInterpolator(getMessageInterpolator(getMessageSource()));
        // 注册 localeResolver
        try {
            this.registerLocaleResolver();
        } catch (ClassNotFoundException e) {
            // WEB_HTTP_CLASS class不存在
            this.registerLocaleContextResolver();
        }

    }

    /**
     * 具体业务具体实现
     *
     * @return Processor
     */
    @ConditionalOnMissingBean({Processor.class})
    @Bean
    public Processor<?> i18nProcessor() {
        return new DefaultProcessor();
    }

    @Bean
    public I18nTemplate<?> i18nTemplate(Processor<?> processor) {
        log.info("Processor type is {}", processor.getClass().getName());
        return new I18nTemplate<>(processor);
    }

    /**
     * 由于 I18nTemplate 有 @ConfigureCache，须等其注册成bean之后，
     * ScanConfig 扫描完成，I18nScanConfig.I18nScanProcessor.dictList 才会有值
     *
     * @param i18nTemplate      i18nTemplate
     * @param i18nScanProcessor i18nScanProcessor
     * @return I18nWrapper
     */
    @Bean
    public I18nWrapper i18nWrapper(I18nTemplate<?> i18nTemplate, I18nScanConfig.I18nScanProcessor i18nScanProcessor) {
        I18nWrapper.setI18nTemplate(i18nTemplate);
        List<Dict4Param> dictList = i18nScanProcessor.getDictList().stream().map(Dict4Param::new).toList();
        I18nWrapper.saveBatch(dictList);
        return new I18nWrapper();
    }

    @Primary
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

    public void registerLocaleResolver() throws ClassNotFoundException {
        try {
            Reflects.classForNameThrow(WEB_HTTP_CLASS);
            this.beanFactory.getBean(LOCALE_RESOLVER_WEB_BEAN, LocaleResolver.class);
        } catch (BeansException e) {
            AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
            localeResolver.setDefaultLocale(Locale.getDefault());
            localeResolver.setSupportedLocales(List.of(Locale.TRADITIONAL_CHINESE, Locale.SIMPLIFIED_CHINESE, Locale.ENGLISH));
            BeanUtil.registerBean(this.beanFactory, LOCALE_RESOLVER_WEB_BEAN, localeResolver);
        }
    }

    public void registerLocaleContextResolver() {
        try {
            this.beanFactory.getBean(LOCALE_RESOLVER_WEB_FLUX_BEAN, LocaleContextResolver.class);
        } catch (BeansException ex) {
            AcceptHeaderLocaleContextResolver localeResolver = new AcceptHeaderLocaleContextResolver();
            localeResolver.setDefaultLocale(Locale.getDefault());
            localeResolver.setSupportedLocales(List.of(Locale.TRADITIONAL_CHINESE, Locale.SIMPLIFIED_CHINESE, Locale.ENGLISH));
            BeanUtil.registerBean(this.beanFactory, LOCALE_RESOLVER_WEB_FLUX_BEAN, localeResolver);
        }
    }
}
