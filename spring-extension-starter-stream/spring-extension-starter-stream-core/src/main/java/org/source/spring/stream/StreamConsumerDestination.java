package org.source.spring.stream;

import org.springframework.cloud.stream.provisioning.ConsumerDestination;

public record StreamConsumerDestination(String name, Listener listener) implements ConsumerDestination {

    @Override
    public String getName() {
        return name;
    }
}