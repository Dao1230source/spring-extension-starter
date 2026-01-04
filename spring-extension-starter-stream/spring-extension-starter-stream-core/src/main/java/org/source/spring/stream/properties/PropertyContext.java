package org.source.spring.stream.properties;

import lombok.Data;
import org.source.spring.stream.template.ConsumerProcessor;
import org.source.spring.stream.template.ProducerProcessor;

import java.util.Objects;
import java.util.function.Supplier;

@Data
public class PropertyContext<C extends ConsumerProcessor, P extends ProducerProcessor> {
    private final String name;
    private Supplier<C> consumerProcessor;
    private Supplier<P> producerProcessor;

    public boolean isConsumer() {
        return Objects.nonNull(consumerProcessor);
    }

    public boolean isProducer() {
        return Objects.nonNull(producerProcessor);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PropertyContext<?, ?> that = (PropertyContext<?, ?>) o;
        return Objects.equals(this.getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }
}
