package org.source.spring.mq.template;

import lombok.extern.slf4j.Slf4j;
import org.source.spring.mq.properties.AbstractMqExtendedBinderProperties;
import org.source.spring.mq.properties.MqConsumerProperties;
import org.source.spring.mq.properties.MqProducerProperties;
import org.springframework.cloud.stream.binder.*;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

@Slf4j
public abstract class AbstractMqBinder<C extends MqConsumerProperties, P extends MqProducerProperties>
        extends AbstractMessageChannelBinder<ExtendedConsumerProperties<C>, ExtendedProducerProperties<P>, MqProvisioningProvider<C, P>>
        implements ExtendedPropertiesBinder<MessageChannel, C, P> {

    private final AbstractMqExtendedBinderProperties<C, P> extendedBinderProperties;

    protected AbstractMqBinder(String[] headersToEmbed, MqProvisioningProvider<C, P> provisioningProvider,
                               AbstractMqExtendedBinderProperties<C, P> extendedBinderProperties) {
        super(headersToEmbed, provisioningProvider);
        this.extendedBinderProperties = extendedBinderProperties;
    }

    @Override
    protected MessageHandler createProducerMessageHandler(ProducerDestination destination,
                                                          ExtendedProducerProperties<P> producerProperties,
                                                          MessageChannel errorChannel) {
        Assert.notNull(destination, "ProducerDestination must not be null");
        Assert.isTrue(destination instanceof MqProducerDestination, "destination must instanceof MqProducerDestination");
        MqProducerDestination mqProducerDestination = (MqProducerDestination) destination;
        String name = mqProducerDestination.getName();
        Assert.hasText(name, "ProducerDestination name must not be empty");
        return message -> {
            /*
             * 接收stream输出通道的消息
             * 转发到消息中间件的生产者
             */
            MqProducer<?, ?> mqProducer = mqProducerDestination.getProducer();
            Assert.notNull(mqProducer, String.format("There is no corresponding KafkaProducer client for name:%s", name));
            Object result = mqProducer.send(message);
            log.debug("producer result :{}", result);
        };
    }

    @Override
    protected MessageProducer createConsumerEndpoint(ConsumerDestination destination,
                                                     String group,
                                                     ExtendedConsumerProperties<C> properties) {
        Assert.notNull(destination, "ConsumerDestination must not be null");
        Assert.isTrue(destination instanceof MqConsumerDestination, "destination must instanceof MqConsumerDestination");
        MqConsumerDestination mqConsumerDestination = (MqConsumerDestination) destination;
        String name = mqConsumerDestination.getName();
        Assert.hasText(name, "ConsumerDestination name must not be empty");
        MqListener listener = mqConsumerDestination.getListener();
        Assert.notNull(listener, String.format("There is no corresponding listener client for name:%s", name));
        return new MessageProducerSupport() {
            @Override
            protected void doStart() {
                listener.setConsumer(this::sendMessage);
            }
        };
    }

    @Override
    public C getExtendedConsumerProperties(String channelName) {
        return extendedBinderProperties.getExtendedConsumerProperties(channelName);
    }

    @Override
    public P getExtendedProducerProperties(String channelName) {
        return extendedBinderProperties.getExtendedProducerProperties(channelName);
    }

    @Override
    public String getDefaultsPrefix() {
        return extendedBinderProperties.getDefaultsPrefix();
    }

    @Override
    public Class<? extends BinderSpecificPropertiesProvider> getExtendedPropertiesEntryClass() {
        return extendedBinderProperties.getExtendedPropertiesEntryClass();
    }
}
