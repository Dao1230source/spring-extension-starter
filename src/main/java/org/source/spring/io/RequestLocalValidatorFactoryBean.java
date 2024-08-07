package org.source.spring.io;

import jakarta.validation.Configuration;
import jakarta.validation.MessageInterpolator;
import jakarta.validation.valueextraction.ValueExtractor;
import org.jetbrains.annotations.NotNull;
import org.source.utility.utils.Streams;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

public class RequestLocalValidatorFactoryBean extends LocalValidatorFactoryBean {
    private final MessageInterpolator messageInterpolator;
    private final List<ValueExtractor<?>> valueExtractorList;

    public RequestLocalValidatorFactoryBean(MessageInterpolator messageInterpolator,
                                            List<ValueExtractor<?>> valueExtractorList) {
        this.messageInterpolator = messageInterpolator;
        this.valueExtractorList = valueExtractorList;
    }

    @Override
    protected void postProcessConfiguration(@NotNull Configuration<?> configuration) {
        Streams.of(valueExtractorList).forEach(configuration::addValueExtractor);
        configuration.messageInterpolator(messageInterpolator);
    }
}
