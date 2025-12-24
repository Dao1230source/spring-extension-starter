package org.source.spring.stream.template;

import lombok.extern.slf4j.Slf4j;
import org.source.spring.stream.PropertiesParser;
import org.source.utility.utils.Maps;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public abstract class AbstractPropertiesHandler<C extends ConsumerProcessor, P extends ProducerProcessor,
        C1 extends AbstractPropertiesHandler.ConsumerProperty,
        P1 extends AbstractPropertiesHandler.ProducerProperty,
        S extends AbstractPropertiesHandler.SystemProperty<C1, P1>>
        implements PropertiesParser<C, P> {

    protected boolean isEnable = true;

    protected abstract Map<String, S> getSystems();

    public interface SystemProperty<C1, P1> {
        boolean isEnable();

        Map<String, C1> getConsumers();

        Map<String, P1> getProducers();
    }

    public interface ProducerProperty {
        boolean isEnable();
    }

    public interface ConsumerProperty {
        boolean isEnable();
    }

    protected abstract P obtainProducer(String systemName, S systemProperty,
                                        String producerName, P1 producerProperty);

    protected abstract C obtainConsumer(String systemName, S systemProperty,
                                        String producerName, C1 consumerProperty);


    @Override
    public Map<String, C> obtainConsumers() {
        if (Maps.isEmpty(this.getSystems())) {
            return Map.of();
        }
        Map<String, C> consumerMap = HashMap.newHashMap(32);
        this.getSystems().forEach((systemName, system) -> {
            if (Maps.isEmpty(system.getConsumers())) {
                return;
            }
            system.getConsumers().forEach((consumerName, consumerProperty) -> {
                if (this.canCreate(this.isEnable, system.isEnable(), consumerProperty.isEnable(), consumerName)) {
                    consumerMap.put(consumerName, this.obtainConsumer(systemName, system, consumerName, consumerProperty));
                }
            });
        });
        return consumerMap;
    }

    @Override
    public Map<String, P> obtainProducers() {
        if (Maps.isEmpty(this.getSystems())) {
            return Map.of();
        }
        Map<String, P> producerMap = HashMap.newHashMap(32);
        this.getSystems().forEach((systemName, system) -> {
            if (Maps.isEmpty(system.getProducers())) {
                return;
            }
            system.getProducers().forEach((producerName, producerProperty) -> {
                if (this.canCreate(isEnable, system.isEnable(), producerProperty.isEnable(), producerName)) {
                    producerMap.put(producerName, this.obtainProducer(systemName, system, producerName, producerProperty));
                }
            });
        });
        return producerMap;
    }

    protected boolean canCreate(boolean isEnable, boolean systemIsEnable, boolean channelIsEnable, String channelName) {
        boolean isEnable1 = isEnable && systemIsEnable && channelIsEnable;
        if (!isEnable1) {
            log.info("{} is disable, all.isEnable:{}, system.isEnable:{}, isEnable:{}",
                    channelName, this.isEnable, systemIsEnable, channelIsEnable);
        }
        return isEnable1;
    }

}