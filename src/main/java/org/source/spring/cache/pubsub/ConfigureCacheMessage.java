package org.source.spring.cache.pubsub;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ConfigureCacheMessage {
    private String cacheName;
    private Boolean isMulti;
    private String message;
}
