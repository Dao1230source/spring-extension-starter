package org.source.spring.stream.template;

import org.source.spring.stream.Producer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.Objects;

public interface KafkaProducer<K, V> extends Producer {

    String getTopicName();

    Class<K> getKeyClass();

    V getValue(Message<?> message);

    void send(String topic, K k, V v);

    void send(String topic, V v);

    /**
     * 将从stream获取到的message发送到实际的producer
     *
     * @param message message
     */
    @Override
    default void send(Message<?> message) {
        String topicName = this.getTopicName();
        V value = getValue(message);
        MessageHeaders headers = message.getHeaders();
        K key = headers.get("kafka_messageKey", getKeyClass());
        // K key = headers.get(KafkaHeaders.KEY, getKeyClass());
        if (Objects.nonNull(key)) {
            send(topicName, key, value);
        } else {
            send(topicName, value);
        }
    }
}