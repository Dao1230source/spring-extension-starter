package org.source.spring.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.source.utility.utils.Jsons;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * @author zengfugen
 */
@AutoConfigureBefore(JacksonAutoConfiguration.class)
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "org.source.spring.enabled", value = "json", matchIfMissing = true)
@AutoConfiguration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper jacksonObjectMapper() {
        return Jsons.getInstance();
    }
}
