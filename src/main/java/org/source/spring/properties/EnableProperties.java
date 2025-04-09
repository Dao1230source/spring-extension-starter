package org.source.spring.properties;

import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 是否启用某个组件，默认false
 */
@Data
@ConfigurationProperties(value = "org.source.spring.enabled")
@AutoConfiguration
public class EnableProperties {
    /**
     * 启用 spring cache configure
     */
    private boolean cache = true;
    private boolean doc = true;
    private boolean exception = true;
    private boolean i18n = true;
    private boolean io = true;
    private boolean json = true;
    private boolean log = true;
    private boolean mq = true;
    private boolean redis = false;
    private boolean lock = false;
    private boolean pubsub = false;
    private boolean redisson = false;
    private boolean request = true;
    private boolean scan = true;
    private boolean uid = false;
    private boolean utility = true;
}
