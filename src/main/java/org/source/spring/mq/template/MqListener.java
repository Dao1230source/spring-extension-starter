package org.source.spring.mq.template;

import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

public interface MqListener {

    /**
     * 设置处理 stream message的Consumer函数
     * {@link AbstractMqBinder#createConsumerEndpoint(ConsumerDestination, String, ExtendedConsumerProperties)}
     *
     * @param consumer consumer
     */
    void setConsumer(Consumer<Message<?>> consumer);
}
