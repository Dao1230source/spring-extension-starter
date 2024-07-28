package org.source.spring.mq.template;

import lombok.Data;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;

@Data
public class MqConsumerDestination implements ConsumerDestination {

    private final String name;
    private final MqListener listener;

    public MqConsumerDestination(String name, MqListener listener) {
        this.name = name;
        this.listener = listener;
    }

    @Override
    public String getName() {
        return name;
    }
}
