package org.source.spring.io.validate;

import jakarta.validation.Configuration;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@AutoConfiguration
public class RequestLocalValidatorFactoryBean extends LocalValidatorFactoryBean {

    @Override
    protected void postProcessConfiguration(@NotNull Configuration<?> configuration) {
        super.postProcessConfiguration(configuration);
        configuration.addValueExtractor(new RequestDataExtractor());
    }
}
