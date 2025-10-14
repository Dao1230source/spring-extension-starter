package org.source.spring.rest;

import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Data
@ConfigurationProperties(value = "org.source.spring.rest")
@AutoConfiguration
public class RestProperties {
    /**
     * {@literal <restName, restProperties>}
     */
    private Map<String, NamedRestProperties> rests;

    @Data
    public static class NamedRestProperties {
        private String baseUrl;
        /**
         * 是否自动将{@see org.source.web.io.Response} 开箱，返回 data
         */
        private boolean autoUnpackResponse = true;
        private boolean autoPackRequest = false;
    }
}