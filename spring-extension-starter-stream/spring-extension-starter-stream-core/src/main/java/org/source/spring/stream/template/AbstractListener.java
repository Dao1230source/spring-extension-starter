package org.source.spring.stream.template;

import lombok.extern.slf4j.Slf4j;
import org.source.spring.stream.Listener;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
* 默认转为 json string 传输
*
* @param <E>
*/
@Slf4j
public abstract class AbstractListener<E> implements Listener {
    protected Consumer<Message<?>> streamMessageConsumer;

    @Override
    public void setConsumer(Consumer<Message<?>> consumer) {
        this.streamMessageConsumer = consumer;
    }

    public void processMessage(E e) {
        if (Objects.nonNull(streamMessageConsumer)) {
            streamMessageConsumer.accept(MessageBuilder.withPayload(e).build());
        }
    }

    public void processMessage(List<E> list) {
        if (Objects.nonNull(streamMessageConsumer)) {
            streamMessageConsumer.accept(MessageBuilder.withPayload(list).build());
        }
    }
}