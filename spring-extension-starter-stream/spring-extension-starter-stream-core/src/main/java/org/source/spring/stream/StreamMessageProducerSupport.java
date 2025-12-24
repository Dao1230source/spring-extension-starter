package org.source.spring.stream;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.util.Assert;

@EqualsAndHashCode(callSuper = true)
@Data
public class StreamMessageProducerSupport<C> extends MessageProducerSupport {
    private final ConsumerDestination destination;
    private final String group;
    private final ExtendedConsumerProperties<C> properties;

    public StreamMessageProducerSupport(ConsumerDestination destination, String group, ExtendedConsumerProperties<C> properties) {
        this.destination = destination;
        this.group = group;
        this.properties = properties;
    }

    @Override
    protected void doStart() {
        Assert.notNull(destination, "ConsumerDestination must not be null");
        Assert.isTrue(destination instanceof StreamConsumerDestination, "destination must instanceof MqConsumerDestination");
        StreamConsumerDestination streamConsumerDestination = (StreamConsumerDestination) destination;
        String name = streamConsumerDestination.getName();
        Assert.hasText(name, "ConsumerDestination name must not be empty");
        Listener listener = streamConsumerDestination.listener();
        Assert.notNull(listener, String.format("There is no corresponding listener client for name:%s", name));
        listener.setConsumer(this::sendMessage);
    }
}