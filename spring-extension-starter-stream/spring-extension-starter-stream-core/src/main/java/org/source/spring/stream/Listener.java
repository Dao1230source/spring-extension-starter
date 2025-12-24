package org.source.spring.stream;

import org.springframework.messaging.Message;

import java.util.function.Consumer;

public interface Listener {

    /**
     * 设置处理 stream message的Consumer函数
     *
     * @param consumer consumer
     */
    void setConsumer(Consumer<Message<?>> consumer);
}