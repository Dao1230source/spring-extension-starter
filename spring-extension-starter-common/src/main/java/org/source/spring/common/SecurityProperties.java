package org.source.spring.common;

import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(value = "org.source.security")
@AutoConfiguration
public class SecurityProperties {
    /**
     * gateway给所有请求添加的请求头 SECRET_KEY
     */
    private String secretKey;
}
