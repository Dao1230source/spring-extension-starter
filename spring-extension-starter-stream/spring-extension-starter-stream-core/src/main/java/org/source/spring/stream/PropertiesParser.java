package org.source.spring.stream;

import org.source.spring.stream.template.ConsumerProcessor;
import org.source.spring.stream.template.ProducerProcessor;

import java.util.Map;

public interface PropertiesParser<C extends ConsumerProcessor, P extends ProducerProcessor> {

    Map<String, C> obtainConsumers();

    Map<String, P> obtainProducers();
}