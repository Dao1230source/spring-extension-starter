package org.source.spring.mq.template;

import lombok.Data;
import lombok.Getter;
import org.springframework.cloud.stream.provisioning.ProducerDestination;

@Data
public class MqProducerDestination implements ProducerDestination {
    private final String name;
    private final int partition;
    @Getter
    private final MqProducer<?, ?> producer;

    public MqProducerDestination(String name, int partition, MqProducer<?, ?> producer) {
        this.name = name;
        this.partition = partition;
        this.producer = producer;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getNameForPartition(int partition) {
        return name;
    }

}
