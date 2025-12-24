package org.source.spring.stream.template;

import org.source.spring.stream.Listener;
import org.springframework.cloud.stream.provisioning.ProvisioningException;

public interface ConsumerProcessor {

    /**
     * 根据配置信息注册消费者并返回 MqListener
     *
     * @return MqConsumer
     */
    Listener createConsumer() throws ProvisioningException;

}