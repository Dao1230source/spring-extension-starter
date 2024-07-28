package org.source.spring.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@Data
@AutoConfiguration
@ConfigurationProperties(value = "org.source.spring.request")
public class RequestProperties {

    /**
     * request-starter 配置
     */
    private Map<String, RetrofitProperties> retrofits;

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class RetrofitProperties {
        private String baseUrl;
        /**
         * 是否自动将{@see org.source.web.io.Response} 开箱，返回 data
         */
        private boolean autoUnpackResponse = true;
        private boolean autoPackRequest = false;
    }
}
