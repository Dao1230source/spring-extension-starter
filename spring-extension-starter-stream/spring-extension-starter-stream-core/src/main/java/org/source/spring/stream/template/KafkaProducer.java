package org.source.spring.stream.template;

import org.jetbrains.annotations.Nullable;
import org.source.spring.stream.Producer;
import org.springframework.messaging.Message;

public interface KafkaProducer<K, V> extends Producer {

    String getTopicName();

    Class<K> getKeyClass();

    default @Nullable K getKey(Message<?> message) {
        return message.getHeaders().get("kafka_messageKey", this.getKeyClass());
    }
}