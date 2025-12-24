package org.source.spring.stream.template;

import org.source.spring.stream.Producer;
import org.springframework.cloud.stream.provisioning.ProvisioningException;

public interface ProducerProcessor {

    /**
     * 根据配置信息创建 producer
     *
     * @return MqProducer
     */
    Producer createProducer() throws ProvisioningException;
}