package org.source.spring.stream.properties;

import org.source.spring.stream.template.ConsumerProcessor;
import org.source.spring.stream.template.ProducerProcessor;

import java.util.HashMap;
import java.util.Map;

public interface PropertiesHandler<C extends ConsumerProcessor, P extends ProducerProcessor> {
    Map<String, PropertyContext<?, ?>> PROPERTY_CONTEXTS = new HashMap<>();

    Map<String, C> obtainConsumers();

    Map<String, P> obtainProducers();
}