package org.source.spring.i18n;

import jakarta.validation.MessageInterpolator;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.resourceloading.CachingResourceBundleLocator;
import org.source.spring.cache.ConfigureCacheConfig;
import org.source.spring.i18n.facade.data.Dict;
import org.source.spring.i18n.facade.param.Dict4Param;
import org.source.spring.i18n.processor.DefaultProcessor;
import org.source.spring.i18n.processor.Processor;
import org.source.spring.i18n.validate.DictMessageSource;
import org.source.spring.i18n.validate.ValidMessageInterpolator;
import org.source.spring.scan.ScanConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.beanvalidation.MessageSourceResourceBundleLocator;

import java.util.List;

@Slf4j
@ConditionalOnProperty(prefix = "org.source.spring.enabled", name = "i18n", matchIfMissing = true)
@AutoConfiguration(after = {ConfigureCacheConfig.class, ScanConfig.class})
public class I18nConfig {

    /**
     * 具体业务具体实现
     *
     * @return Processor
     */
    @ConditionalOnMissingBean({Processor.class})
    @Bean
    public Processor<? extends Dict> i18nProcessor() {
        return new DefaultProcessor();
    }

    @Bean
    public I18nTemplate<? extends Dict> i18nTemplate(Processor<?> processor) {
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
