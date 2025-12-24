package org.source.spring.stream;

import org.springframework.cloud.stream.provisioning.ProducerDestination;

public record StreamProducerDestination(String name, int partition, Producer producer) implements ProducerDestination {

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getNameForPartition(int partition) {
        return name;
    }

}