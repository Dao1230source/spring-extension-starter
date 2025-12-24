package org.source.spring.stream;

import org.springframework.messaging.Message;

public interface Producer {
    /**
     * 将从stream获取到的message发送到实际的producer
     *
     * @param message message
     */
    void send(Message<?> message);
}