package org.source.spring.cache.configure;

import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(value = "org.source.cache")
@AutoConfiguration
public class ConfigureTtlProperties {
    private Long redisTtl;
    private Long jvmTtl;
}
