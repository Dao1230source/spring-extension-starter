package org.source.spring.stream.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.source.spring.stream.Producer;
import org.springframework.messaging.Message;

@AllArgsConstructor
@Data
public class RestProducer implements Producer {
    private final String channelName;

    @Override
    public void send(Message<?> message) {

    }
}