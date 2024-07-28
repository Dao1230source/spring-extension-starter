package org.source.spring.mq.template;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.source.utility.utils.Jsons;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Slf4j
public abstract class AbstractMqListener<E> implements MqListener {
    protected Consumer<Message<?>> consumer;

    @Override
    public void setConsumer(Consumer<Message<?>> consumer) {
        this.consumer = consumer;
    }

    public abstract byte[] mqMessageToBytes(E e);

    /**
     * 将 {@literal Message<K, V>}数据转为 stream的Message
     *
     * @param list list
     * @return message
     */
    protected Message<?> convert(List<E> list) {
        if (Objects.isNull(list) || list.isEmpty()) {
            return MessageBuilder.withPayload(List.of()).build();
        }
        List<?> collect = list.stream().map(e -> {
            byte[] value = this.mqMessageToBytes(e);
            JsonNode jsonNode;
            try {
                jsonNode = Jsons.getInstance().readTree(value);
                // 如果是数组，转为list
                if (jsonNode.isArray()) {
                    List<String> strList = new ArrayList<>(jsonNode.size());
                    for (JsonNode node : jsonNode) {
                        strList.add(node.toString());
                    }
                    return strList;
                }
            } catch (IOException ex) {
                log.warn("MeshListener convert exception", ex);
            }
            return List.of(new String(value));
        }).flatMap(Collection::stream).toList();
        return MessageBuilder.withPayload(collect).build();
    }
}
