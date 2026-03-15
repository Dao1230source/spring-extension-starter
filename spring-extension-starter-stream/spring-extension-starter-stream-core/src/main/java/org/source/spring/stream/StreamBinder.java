package org.source.spring.stream;

import lombok.extern.slf4j.Slf4j;
import org.source.spring.common.exception.SpExtExceptionEnum;
import org.source.spring.stream.template.ConsumerProcessor;
import org.source.spring.stream.template.ProducerProcessor;
import org.springframework.cloud.stream.binder.*;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.integration.core.MessageProducer;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Slf4j
public class StreamBinder<C extends ConsumerProcessor, P extends ProducerProcessor>
        extends AbstractMessageChannelBinder<ExtendedConsumerProperties<C>, ExtendedProducerProperties<P>, StreamProvisioningProvider<C, P>>
        implements ExtendedPropertiesBinder<MessageChannel, C, P> {

    private final StreamBinderProperties<C, P> extendedBinderProperties;

    public StreamBinder(@Nullable String[] headersToEmbed, StreamProvisioningProvider<C, P> provisioningProvider,
                        StreamBinderProperties<C, P> extendedBinderProperties) {
        super(headersToEmbed, provisioningProvider);
        this.extendedBinderProperties = extendedBinderProperties;
    }

    @Override
    protected MessageHandler createProducerMessageHandler(ProducerDestination destination,
                                                          ExtendedProducerProperties<P> producerProperties,
                                                          MessageChannel errorChannel) {
        return new StreamMessageHandler<>(destination, producerProperties, errorChannel);
    }

    @Override
    protected MessageProducer createConsumerEndpoint(ConsumerDestination destination,
                                                     String group,
                                                     ExtendedConsumerProperties<C> properties) {
        return new StreamMessageProducerSupport<>(destination, group, properties);
    }

    @Override
    public C getExtendedConsumerProperties(String channelName) {
        return extendedBinderProperties.getExtendedConsumerProperties(channelName);
    }

    @Override
    public P getExtendedProducerProperties(String channelName) {
        P extendedProducerProperties = extendedBinderProperties.getExtendedProducerProperties(channelName);
        SpExtExceptionEnum.STREAM_PRODUCER_PROCESSOR_NOT_FOUND.nonNull(extendedProducerProperties, channelName);
        return extendedProducerProperties;
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