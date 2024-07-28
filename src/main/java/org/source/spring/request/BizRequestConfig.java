package org.source.spring.request;

import lombok.AllArgsConstructor;
import org.source.spring.properties.SecurityProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

@AllArgsConstructor
@ConditionalOnBean(SecurityProperties.class)
@AutoConfigureAfter
@AutoConfiguration
public class BizRequestConfig {
    private final SecurityProperties securityProperties;

    @Bean
    public RequestInterceptor traceInterceptor() {
        return new RequestInterceptor(securityProperties);
    }
}
