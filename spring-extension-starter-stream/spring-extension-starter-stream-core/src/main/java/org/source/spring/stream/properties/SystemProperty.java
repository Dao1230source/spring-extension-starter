package org.source.spring.stream.properties;

import java.util.Map;

public interface SystemProperty<C1 extends ConsumerProperty, P1 extends ProducerProperty> extends Property {

    Map<String, C1> getConsumers();

    Map<String, P1> getProducers();
}
