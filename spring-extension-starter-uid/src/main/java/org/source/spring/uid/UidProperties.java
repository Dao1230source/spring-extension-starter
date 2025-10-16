package org.source.spring.uid;

import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@ConfigurationProperties(value = "org.source.uid")
@AutoConfiguration
public class UidProperties {
    /**
     * yyyy-MM-dd HH:mm:ss
     */
    private String startDate = "2025-01-01 00:00:00";
    private int nodeIdBits = 10;
    private int sequenceBits = 12;

}
