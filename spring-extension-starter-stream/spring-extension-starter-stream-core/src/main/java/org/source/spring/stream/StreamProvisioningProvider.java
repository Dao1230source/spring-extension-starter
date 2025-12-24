package org.source.spring.stream;

import lombok.extern.slf4j.Slf4j;
import org.source.spring.stream.template.ConsumerProcessor;
import org.source.spring.stream.template.ProducerProcessor;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningException;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;

@Slf4j
public class StreamProvisioningProvider<C extends ConsumerProcessor, P extends ProducerProcessor>
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
    public ProducerDestination provisionProducerDestination(String name, ExtendedProducerProperties<P> properties) throws ProvisioningException {
        P producerProperties = properties.getExtension();
        Producer producer = producerProperties.createProducer();
        log.info("message producerDestination:{} register successfully", name);
        return new StreamProducerDestination(name, 0, producer);
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
        C consumerProperties = properties.getExtension();
        Listener listener = consumerProperties.createConsumer();
        log.info("message consumerDestination:{} register successfully", name);
        return new StreamConsumerDestination(name, listener);
    }

}