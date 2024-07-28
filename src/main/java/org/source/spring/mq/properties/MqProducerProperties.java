package org.source.spring.mq.properties;

import org.source.spring.mq.template.MqProducer;
import org.springframework.cloud.stream.provisioning.ProvisioningException;

public interface MqProducerProperties {

    /**
     * 根据配置信息创建 producer
     *
     * @return MqProducer
     */
    MqProducer<?, ?> createProducer() throws ProvisioningException;
}
