package org.source.spring.stream.template;

import lombok.extern.slf4j.Slf4j;
import org.source.spring.stream.properties.*;
import org.source.utility.utils.Maps;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public abstract class AbstractPropertiesHandler<C extends ConsumerProcessor, P extends ProducerProcessor,
        C1 extends ConsumerProperty, P1 extends ProducerProperty, S extends SystemProperty<C1, P1>>
        implements PropertiesHandler<C, P> {

    protected boolean isEnable = true;

    public abstract Map<String, S> getSystems();

    /**
     * 全量处理时使用
     *
     * @param systemName       系统名称
     * @param systemProperty   系统公用配置
     * @param producerName     生产者名称
     * @param producerProperty 生产者配置
     * @return P
     */
    protected abstract P obtainProducer(String systemName, S systemProperty,
                                        String producerName, P1 producerProperty);

    protected abstract C obtainConsumer(String systemName, S systemProperty,
                                        String consumerName, C1 consumerProperty);

    /**
     * 单个处理时使用，如配置刷新时只销毁/创建单个的生产者或消费者
     *
     * @param systemName   系统名称
     * @param producerName 生产者名称
     * @return P
     */
    @Nullable
    protected P obtainProducer(String systemName, String producerName) {
        return null;
    }

    @Nullable
    protected P obtainConsumer(String systemName, String consumerName) {
        return null;
    }

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
                    String contextName = String.join(".", systemName, consumerName);
                    PropertyContext<C, P> context = new PropertyContext<>(contextName);
                    context.setConsumerProcessor(() -> this.obtainConsumer(systemName, system, consumerName, consumerProperty));
                    PropertiesHandler.PROPERTY_CONTEXTS.put(context.getName(), context);
                    consumerMap.put(consumerName, context.getConsumerProcessor().get());
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
                    String contextName = String.join(".", systemName, producerName);
                    PropertyContext<C, P> context = new PropertyContext<>(contextName);
                    context.setProducerProcessor(() -> this.obtainProducer(systemName, system, producerName, producerProperty));
                    PropertiesHandler.PROPERTY_CONTEXTS.put(context.getName(), context);
                    producerMap.put(producerName, context.getProducerProcessor().get());
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