package org.source.spring.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ConfigurationProperties(value = "org.source.spring.cache")
@AutoConfiguration
public class ConfigureTtlProperties {
    private Long redisTtl;
    private Long jvmTtl;
}
