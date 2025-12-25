package org.source.spring.stream.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.source.spring.stream.Producer;
import org.source.spring.stream.template.ProducerProcessor;
import org.springframework.cloud.stream.provisioning.ProvisioningException;

@AllArgsConstructor
@Data
public class RestProducerProcessor implements ProducerProcessor {
    private final String channelName;

    @Override
    public Producer createProducer() throws ProvisioningException {
        return new RestProducer(channelName);
    }
}