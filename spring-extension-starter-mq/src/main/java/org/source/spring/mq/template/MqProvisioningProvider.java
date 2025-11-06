package org.source.spring.mq.template;

import lombok.extern.slf4j.Slf4j;
import org.source.spring.mq.properties.MqConsumerProperties;
import org.source.spring.mq.properties.MqProducerProperties;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningException;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;

@Slf4j
public abstract class MqProvisioningProvider<C extends MqConsumerProperties, P extends MqProducerProperties>
        implements ProvisioningProvider<ExtendedConsumerProperties<C>, ExtendedProducerProperties<P>> {

    /**
     * 读取配置，生成生产者客户端
     *
     * @param name       绑定通道名称
     * @param properties stream生产者的配置信息
     * @return 生产者目的地
     * @throws ProvisioningException ProvisioningException
     */
    @Override
    public ProducerDestination provisionProducerDestination(String name, ExtendedProducerProperties<P> properties)
            throws ProvisioningException {
        P mqProducerProperties = properties.getExtension();
        MqProducer<?, ?> mqProducer = mqProducerProperties.createProducer();
        log.info("message queue producerDestination:{} register successfully", name);
        return new MqProducerDestination(name, 0, mqProducer);
    }

    /**
     * 读取配置，生成消费者客户端
     *
     * @param name       绑定通道名称
     * @param group      分组，sf-kafka未实现
     * @param properties stream消费者的配置信息
     * @return 消费者目的地
     * @throws ProvisioningException ProvisioningException
     */
    @Override
    public ConsumerDestination provisionConsumerDestination(String name, String group, ExtendedConsumerProperties<C> properties) throws ProvisioningException {
        C mqConsumerProperties = properties.getExtension();
        MqListener mqListener = mqConsumerProperties.registerConsumer();
        log.info("message queue consumerDestination:{} register successfully", name);
        return new MqConsumerDestination(name, mqListener);
    }

}
