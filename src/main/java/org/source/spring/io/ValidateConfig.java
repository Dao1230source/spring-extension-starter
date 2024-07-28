package org.source.spring.io;

import jakarta.validation.Validator;
import org.source.spring.io.validate.RequestLocalValidatorFactoryBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationConfigurationCustomizer;
import org.springframework.boot.validation.MessageInterpolatorFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ConditionalOnProperty(prefix = "org.source.spring", name = "io", matchIfMissing = true)
@AutoConfiguration(before = {ValidationAutoConfiguration.class})
public class ValidateConfig {

    /**
     * 在此之前创建 bean {@link ValidationAutoConfiguration#defaultValidator(ApplicationContext, ObjectProvider)}
     * 不是webFlux项目加载
     *
     * @param applicationContext applicationContext
     * @param customizers        customizers
     * @return Validator
     */
    @Primary
    @ConditionalOnMissingClass(value = "org.springframework.web.reactive.DispatcherHandler")
    @Bean
    public Validator defaultValidator(ApplicationContext applicationContext,
                                      ObjectProvider<ValidationConfigurationCustomizer> customizers) {
        LocalValidatorFactoryBean factoryBean = new RequestLocalValidatorFactoryBean();
        factoryBean.setConfigurationInitializer(configuration -> customizers.orderedStream()
                .forEach(customizer -> customizer.customize(configuration)));
        MessageInterpolatorFactory interpolatorFactory = new MessageInterpolatorFactory(applicationContext);
        factoryBean.setMessageInterpolator(interpolatorFactory.getObject());
        return factoryBean;
    }

}
