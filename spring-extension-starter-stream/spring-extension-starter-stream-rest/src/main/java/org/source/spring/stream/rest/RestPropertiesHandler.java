package org.source.spring.stream.rest;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.source.spring.stream.template.AbstractPropertiesHandler;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Validated
@ConfigurationProperties("spring.cloud.stream")
public class RestPropertiesHandler extends AbstractPropertiesHandler<RestConsumerProcessor, RestProducerProcessor,
        RestPropertiesHandler.RestConsumer, RestPropertiesHandler.RestProducer,
        RestPropertiesHandler.RestSystem> {

    @Setter
    private RequestMappingHandlerMapping handlerMapping;

    @Setter
    @Getter
    private Map<String, RestSystem> systems;

    @Data
    public static class RestSystem implements SystemProperty<RestConsumer, RestProducer> {
        private boolean isEnable = true;

        private Map<String, RestProducer> producers;
        private Map<String, RestConsumer> consumers;
    }

    @Data
    public static class RestProducer implements ProducerProperty {
        private boolean isEnable = true;
    }

    @Override
    protected RestProducerProcessor obtainProducer(String systemName, RestSystem systemProperty,
                                                   String producerName, RestProducer producerProperty) {
        return new RestProducerProcessor(producerName);
    }

    @Data
    public static class RestConsumer implements ConsumerProperty {
        private boolean isEnable = true;
        private String path;
    }

    @Override
    protected RestConsumerProcessor obtainConsumer(String systemName, RestSystem systemProperty,
                                                   String producerName, RestConsumer consumerProperty) {
        return new RestConsumerProcessor(this.handlerMapping, consumerProperty.getPath());
    }
}