package org.source.spring.stream.rest;

import org.source.spring.stream.StreamBinder;
import org.source.spring.stream.StreamBinderProperties;
import org.source.spring.stream.StreamProvisioningProvider;
import org.source.spring.stream.converter.StringMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@EnableConfigurationProperties(RestPropertiesHandler.class)
@AutoConfiguration
public class RestBinderConfiguration {

    @Order(-1)
    @ConditionalOnMissingBean
    @Bean
    public MessageConverter stringMessageConverter() {
        return new StringMessageConverter();
    }

    @Bean
    public StreamProvisioningProvider<RestConsumerProcessor, RestProducerProcessor> restProvisioningProvider() {
        return new StreamProvisioningProvider<>();
    }

    @Bean
    public StreamBinderProperties<RestConsumerProcessor, RestProducerProcessor> restBinderProperties(
            RestPropertiesHandler restPropertiesHandler,
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        restPropertiesHandler.setHandlerMapping(handlerMapping);
        return new StreamBinderProperties<>(restPropertiesHandler);
    }

    @Bean
    public StreamBinder<RestConsumerProcessor, RestProducerProcessor> restBinder(
            StreamProvisioningProvider<RestConsumerProcessor, RestProducerProcessor> restProvisioningProvider,
            StreamBinderProperties<RestConsumerProcessor, RestProducerProcessor> restBinderProperties) {
        return new StreamBinder<>(null, restProvisioningProvider, restBinderProperties);
    }
}