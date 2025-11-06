package org.source.spring.mq.properties;

import org.source.spring.mq.template.MqListener;
import org.springframework.cloud.stream.provisioning.ProvisioningException;

public interface MqConsumerProperties {

    /**
     * 根据配置信息注册消费者并返回 MqListener
     *
     * @return MqConsumer
     */
    MqListener registerConsumer() throws ProvisioningException;

}
