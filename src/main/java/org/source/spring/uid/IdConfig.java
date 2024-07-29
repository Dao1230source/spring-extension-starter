package org.source.spring.uid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.source.spring.scan.ScanConfig;
import org.source.spring.utility.SystemUtil;
import org.source.utility.utils.Dates;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "org.source.spring.enabled", name = "uid", matchIfMissing = true)
@AutoConfigureBefore(ScanConfig.class)
@EnableConfigurationProperties(IdProperties.class)
@AutoConfiguration
public class IdConfig {
    public static final String NODE_ID_KEY = "unique_id::node_id::key";
    public static final String NODE_ID_SCRIPT = """
            local key = KEYS[1]
             local nodeName = ARGV[1]
             local nodeId = redis.call('ZSCORE', key, nodeName)
             if (nodeId) then
                 return nodeId
             else
                 local maxNodeWithScores = redis.call('ZRANGE', key, -1, -1, 'WITHSCORES')
                 local nextNodeId = 0
                 if (#maxNodeWithScores > 0) then
                     nextNodeId = tonumber(maxNodeWithScores[2]) + 1
                 end
                 redis.call('ZADD', key, nextNodeId, nodeName)
                 return redis.call('ZSCORE', key, nodeName)
             end
            """;
    private final IdProperties idProperties;
    private final StringRedisTemplate redisTemplate;


    @ConditionalOnMissingBean
    @Bean
    public IdGenerator idGenerator() {
        LocalDateTime localDateTime = Dates.strToLocalDateTime(idProperties.getStartDate());
        long startTimestamp = Dates.localDateTimeToMilli(localDateTime);
        String nodeName = SystemUtil.getIp() + SystemUtil.getPort();
        String nodeId = redisTemplate.execute(RedisScript.of(NODE_ID_SCRIPT, String.class), Collections.singletonList(NODE_ID_KEY), nodeName);
        Assert.notNull(nodeId, "get nodeId null from redis");
        log.debug("nodeName:{}, nodeId:{}", nodeName, nodeId);
        IdGenerator idGenerator = new IdGenerator(Long.parseLong(nodeId), startTimestamp, idProperties.getNodeIdBits(), idProperties.getSequenceBits());
        Ids.setIdGenerator(idGenerator);
        return idGenerator;
    }

}
