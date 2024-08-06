package org.source.spring.io;

import jakarta.validation.Validator;
import org.source.spring.io.validate.RequestLocalValidatorFactoryBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationConfigurationCustomizer;
import org.springframework.boot.validation.MessageInterpolatorFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ConditionalOnProperty(prefix = "org.source.spring.enabled", name = "io", matchIfMissing = true)
@AutoConfiguration(before = {ValidationAutoConfiguration.class})
public class ValidateConfig {

    /**
     * 设置优先使用该bean
     * <p>
     * web mvc {@see WebMvcAutoConfiguration.EnableWebMvcConfiguration#mvcValidator()}
     * webFlux{@see WebFluxAutoConfiguration.EnableWebFluxConfiguration#webFluxValidator()}
     *
     * @return LocalValidatorFactoryBean
     */
    @Primary
    @Bean
    public LocalValidatorFactoryBean localValidatorFactoryBean() {
        return new RequestLocalValidatorFactoryBean();
    }

    /**
     * 在此之前创建 bean {@link ValidationAutoConfiguration#defaultValidator(ApplicationContext, ObjectProvider)}
     * 不是webFlux项目加载
     *
     * @param applicationContext applicationContext
     * @param customizers        customizers
     * @return Validator
     */
    @Bean
    public Validator defaultValidator(ApplicationContext applicationContext,
                                      ObjectProvider<ValidationConfigurationCustomizer> customizers,
                                      LocalValidatorFactoryBean factoryBean) {
        factoryBean.setConfigurationInitializer(configuration -> customizers.orderedStream()
                .forEach(customizer -> customizer.customize(configuration)));
        MessageInterpolatorFactory interpolatorFactory = new MessageInterpolatorFactory(applicationContext);
        factoryBean.setMessageInterpolator(interpolatorFactory.getObject());
        return factoryBean;
    }
}
