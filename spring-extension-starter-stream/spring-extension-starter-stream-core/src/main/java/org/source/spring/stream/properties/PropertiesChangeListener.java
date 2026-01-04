package org.source.spring.stream.properties;

import lombok.extern.slf4j.Slf4j;
import org.source.spring.common.utility.SpringUtil;
import org.source.spring.stream.StreamBinderProperties;
import org.source.spring.stream.template.ConsumerProcessor;
import org.source.spring.stream.template.ProducerProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;

import java.util.Map;

@Slf4j
@AutoConfiguration
public class PropertiesChangeListener implements ApplicationListener<EnvironmentChangeEvent> {
    @SuppressWarnings("unchecked")
    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        // todo 查看key具体是哪些值，看怎么从AbstractPropertiesHandler取值
        if (!event.getKeys().isEmpty()) {
            log.info("received properties change event: {}", event);
            System.out.println("app.message 已更新");
        }
        try {
            StreamBinderProperties<ConsumerProcessor, ProducerProcessor> binderProperties =
                    SpringUtil.getBean(StreamBinderProperties.class);
            PropertyContext<ConsumerProcessor, ProducerProcessor> context
                    = (PropertyContext<ConsumerProcessor, ProducerProcessor>) PropertiesHandler.PROPERTY_CONTEXTS.get("");
            if (context.isConsumer()) {
                binderProperties.refreshConsumers(Map.of(context.getName(), context.getConsumerProcessor().get()));
            } else if (context.isProducer()) {
                binderProperties.refreshProducers(Map.of(context.getName(), context.getProducerProcessor().get()));
            }
            // todo stream 重新注册 Destination
        } catch (Exception e) {
            log.debug("no Stream binder properties found");
        }
    }
}