package org.source.spring.stream.route;

import lombok.extern.slf4j.Slf4j;
import org.source.spring.common.AbstractImportRegistrar;
import org.source.spring.common.spel.SpElUtil;
import org.source.spring.common.utility.ImportRegistrarUtil;
import org.source.spring.stream.properties.ConsumerProperty;
import org.source.spring.stream.properties.ProducerProperty;
import org.source.spring.stream.properties.Router;
import org.source.spring.stream.properties.SystemProperty;
import org.source.spring.stream.template.AbstractPropertiesHandler;
import org.source.spring.stream.template.ConsumerProcessor;
import org.source.spring.stream.template.ProducerProcessor;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Streams;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class StreamRouterImportRegistrar extends AbstractImportRegistrar {
    public StreamRouterImportRegistrar() {
        super("cache");
    }

    @Override
    protected void registerBeanDefinitionsAfterContainerReady(AnnotationMetadata importingClassMetadata,
                                                              BeanDefinitionRegistry registry,
                                                              ApplicationContext applicationContext) {
        log.debug("ImportBeanDefinitionRegistrar for @EnableExtendedStreamRouter");
        @SuppressWarnings("unchecked")
        AbstractPropertiesHandler<ConsumerProcessor, ProducerProcessor,
                ConsumerProperty, ProducerProperty, SystemProperty<ConsumerProperty, ProducerProperty>> bean =
                applicationContext.getBean(AbstractPropertiesHandler.class);
        Map<String, SystemProperty<ConsumerProperty, ProducerProperty>> systems = bean.getSystems();
        systems.values().stream().map(SystemProperty::getConsumers).forEach(consumers ->
                consumers.forEach((name, consumer) -> {
                    if (CollectionUtils.isEmpty(consumer.getRouters())) {
                        return;
                    }
                    String beanName = name;
                    int idx = name.lastIndexOf('-');
                    if (idx > 0) {
                        beanName = name.substring(0, idx);
                    }
                    Consumer<List<String>> listConsumer = this.forwardMessage(applicationContext, consumer.getRouters());
                    ImportRegistrarUtil.registerBeanDefinition(registry, Consumer.class, beanName, () -> listConsumer);
                }));
    }

    protected Consumer<List<String>> forwardMessage(ApplicationContext applicationContext, List<Router> routers) {
        return list -> {
            log.debug("receive messages: {}", list);
            routers.forEach(router -> {
                try {
                    this.processRouter(list, router, applicationContext);
                } catch (Exception e) {
                    log.error("Failed to forward the message，router：{}", router, e);
                }
            });
        };
    }

    protected void processRouter(List<String> list, Router router, ApplicationContext applicationContext) {
        List<String> toSendMessage = Streams.retain(list, k -> {
            if (!StringUtils.hasText(k)) {
                return true;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = Jsons.obj(k, Map.class);
            Boolean parse = SpElUtil.parse(router.getCondition(), Boolean.class,
                    context -> context.setVariables(map));
            return Boolean.TRUE.equals(parse);
        }).toList();
        StreamBridge streamBridge = applicationContext.getBean(StreamBridge.class);
        streamBridge.send(router.getProducerName(), toSendMessage);
    }

}