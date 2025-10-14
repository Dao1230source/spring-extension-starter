package org.source.spring.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class RestProperties {
    private String baseUrl;
    /**
     * 是否自动将{@see org.source.web.io.Response} 开箱，返回 data
     */
    private boolean autoUnpackResponse = true;
    private boolean autoPackRequest = false;
}
