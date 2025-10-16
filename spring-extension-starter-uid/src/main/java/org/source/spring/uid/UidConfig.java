package org.source.spring.uid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.source.spring.common.utility.SystemUtil;
import org.source.utility.utils.Dates;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.Collections;

@Slf4j
@RequiredArgsConstructor
@AutoConfiguration
public class UidConfig {
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
    private final UidProperties uidProperties;
    private final StringRedisTemplate redisTemplate;


    @ConditionalOnMissingBean
    @Bean
    public UidGenerator idGenerator() {
        LocalDateTime localDateTime = Dates.strToLocalDateTime(uidProperties.getStartDate());
        long startTimestamp = Dates.localDateTimeToMilli(localDateTime);
        String nodeName = SystemUtil.getIp() + SystemUtil.getPort();
        String nodeId = redisTemplate.execute(RedisScript.of(NODE_ID_SCRIPT, String.class), Collections.singletonList(NODE_ID_KEY), nodeName);
        Assert.notNull(nodeId, "get nodeId null from redis");
        log.debug("nodeName:{}, nodeId:{}", nodeName, nodeId);
        UidGenerator uidGenerator = new UidGenerator(Long.parseLong(nodeId), startTimestamp, uidProperties.getNodeIdBits(), uidProperties.getSequenceBits());
        Uids.setUidGenerator(uidGenerator);
        return uidGenerator;
    }

}
