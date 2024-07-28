package org.source.spring.properties;

import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 是否启用某个组件
 */
@Data
@ConfigurationProperties(value = "org.source.spring")
@AutoConfiguration
public class EnableProperties {
    /**
     * 启用 spring cache configure
     */
    private boolean cache = true;
    private boolean exception = true;
    private boolean i18n = true;
    private boolean io = true;
    private boolean json = true;
    private boolean mq = true;
    private boolean redis = true;
    private boolean lock = true;
    private boolean pubsub = true;
    private boolean request = true;
    private boolean scan = true;
    private boolean uid = true;
    private boolean utility = true;
}
