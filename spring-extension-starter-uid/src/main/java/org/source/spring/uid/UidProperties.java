package org.source.spring.uid;

import lombok.Data;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
* 如果同一组系统集群使用同一个id生产器，配置参数都应一致
*/
@Data
@ConfigurationProperties(value = "org.source.uid")
@AutoConfiguration
public class UidProperties {
    /**
     * yyyy-MM-dd HH:mm:ss
     */
    private String startDate = "2025-01-01 00:00:00";
    /**
     * 机器码位长 默认值6，取值范围 [1, 15]（要求：序列数位长+机器码位长不超过22）
     */
    private byte nodeIdBits = 6;
    /**
     * 序列数位长 默认值6，取值范围 [3, 21]（要求：序列数位长+机器码位长不超过22）
     */
    private byte sequenceBits = 6;

}