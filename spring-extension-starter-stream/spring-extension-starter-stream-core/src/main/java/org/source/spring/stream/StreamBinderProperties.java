package org.source.spring.stream;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.source.spring.stream.template.ConsumerProcessor;
import org.source.spring.stream.template.ProducerProcessor;
import org.springframework.cloud.stream.binder.BinderSpecificPropertiesProvider;
import org.springframework.cloud.stream.binder.ExtendedBindingProperties;

import java.util.Map;

@Data
@AllArgsConstructor
public class StreamBinderProperties<C extends ConsumerProcessor, P extends ProducerProcessor>
        implements ExtendedBindingProperties<C, P> {

    private Map<String, C> consumers;
    private Map<String, P> producers;

    private PropertiesParser<C, P> propertiesParser;

    public StreamBinderProperties(PropertiesParser<C, P> propertiesParser) {
        this.propertiesParser = propertiesParser;
        this.consumers = propertiesParser.obtainConsumers();
        this.producers = propertiesParser.obtainProducers();
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
}