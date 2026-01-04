package org.source.spring.stream;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.source.spring.stream.properties.PropertiesHandler;
import org.source.spring.stream.template.ConsumerProcessor;
import org.source.spring.stream.template.ProducerProcessor;
import org.springframework.cloud.stream.binder.BinderSpecificPropertiesProvider;
import org.springframework.cloud.stream.binder.ExtendedBindingProperties;

import java.util.Map;

@Getter
@AllArgsConstructor
public class StreamBinderProperties<C extends ConsumerProcessor, P extends ProducerProcessor>
        implements ExtendedBindingProperties<C, P> {

    private Map<String, C> consumers;
    private Map<String, P> producers;

    private PropertiesHandler<C, P> propertiesHandler;

    public StreamBinderProperties(PropertiesHandler<C, P> propertiesHandler) {
        this.propertiesHandler = propertiesHandler;
        this.consumers = propertiesHandler.obtainConsumers();
        this.producers = propertiesHandler.obtainProducers();
    }

    @Override
    public String getDefaultsPrefix() {
        return "spring.cloud.stream.default";
    }

    @Override
    public C getExtendedConsumerProperties(String binding) {
        return this.consumers.get(binding);
    }

    @Override
    public P getExtendedProducerProperties(String binding) {
        return this.producers.get(binding);
    }

    @Override
    public Class<? extends BinderSpecificPropertiesProvider> getExtendedPropertiesEntryClass() {
        return BinderSpecificPropertiesProvider.class;
    }

    public void refreshConsumers(Map<String, C> consumers) {
        this.consumers.putAll(consumers);
    }

    public void refreshProducers(Map<String, P> producers) {
        this.producers.putAll(producers);
    }
}