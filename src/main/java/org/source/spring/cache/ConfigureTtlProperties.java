package org.source.spring.cache;

import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConditionalOnProperty(prefix = "org.source.spring.enabled", name = "cache", havingValue = "true", matchIfMissing = true)
@ConfigurationProperties(value = "org.source.spring.cache")
@AutoConfiguration
public class ConfigureTtlProperties {
    private Long redisTtl;
    private Long jvmTtl;
}
