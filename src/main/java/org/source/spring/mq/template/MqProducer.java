package org.source.spring.mq.template;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.source.utility.utils.Jsons;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.Objects;

public interface MqProducer<K, V> {
    Logger log = LoggerFactory.getLogger(MqProducer.class);

    String getTopicName();

    Class<K> getKeyClass();

    V getValue(Message<?> message);

    Object send(String topic, K k, V v);

    Object send(String topic, V v);

    /**
     * 将从stream获取到的message发送到实际的producer
     *
     * @param message message
     * @return result
     */
    default Object send(Message<?> message) {
        String topicName = this.getTopicName();
        V value = getValue(message);
        MessageHeaders headers = message.getHeaders();
        K key = headers.get("kafka_messageKey", getKeyClass());
        if (Objects.nonNull(key)) {
            return send(topicName, key, value);
        } else {
            return send(topicName, value);
        }
    }

    static byte[] valueToBytes(Message<?> message) {
        return Jsons.bytes(message.getPayload());
    }
}
