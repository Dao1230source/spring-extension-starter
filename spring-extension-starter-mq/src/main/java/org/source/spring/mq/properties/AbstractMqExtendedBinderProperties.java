package org.source.spring.mq.properties;

import lombok.Data;
import lombok.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.stream.binder.BinderSpecificPropertiesProvider;
import org.springframework.cloud.stream.binder.ExtendedBindingProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import java.util.HashMap;
import java.util.Map;

@Data
@ConfigurationProperties("spring.cloud.stream")
@AutoConfiguration
public abstract class AbstractMqExtendedBinderProperties<C, P>
        implements ExtendedBindingProperties<C, P>, ApplicationContextAware {

    private Map<String, C> consumers = new HashMap<>();
    private Map<String, P> producers = new HashMap<>();

    private ConfigurableApplicationContext applicationContext = new GenericApplicationContext();

    @Override
    public String getDefaultsPrefix() {
        return "spring.cloud.stream.default";
    }

    @Override
    public C getExtendedConsumerProperties(String binding) {
        return this.consumers.get(binding);
    }

    @Override
    public P getExtendedProducerProperties(String binding) {
        return this.producers.get(binding);
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public Class<? extends BinderSpecificPropertiesProvider> getExtendedPropertiesEntryClass() {
        return BinderSpecificPropertiesProvider.class;
    }
}
