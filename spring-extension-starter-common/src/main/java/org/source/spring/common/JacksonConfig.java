package org.source.spring.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.source.utility.utils.Jsons;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * @author zengfugen
 */
@AutoConfigureBefore(JacksonAutoConfiguration.class)
@ConditionalOnWebApplication
@AutoConfiguration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper jacksonObjectMapper() {
        return Jsons.getInstance();
    }
}
