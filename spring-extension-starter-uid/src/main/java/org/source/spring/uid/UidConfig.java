package org.source.spring.uid;

import com.github.yitter.contract.IdGeneratorOptions;
import com.github.yitter.idgen.YitIdHelper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.source.spring.common.utility.SystemUtil;
import org.source.utility.utils.Dates;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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

    @PostConstruct
    public void init() {
        LocalDateTime localDateTime = Dates.strToLocalDateTime(uidProperties.getStartDate());
        long startTimestamp = Dates.localDateTimeToMilli(localDateTime);
        String nodeName = SystemUtil.getIp() + SystemUtil.getPort();
        String nodeId = redisTemplate.execute(RedisScript.of(NODE_ID_SCRIPT, String.class), Collections.singletonList(NODE_ID_KEY), nodeName);
        Assert.notNull(nodeId, "get nodeId null from redis");
        log.debug("nodeName:{}, nodeId:{}", nodeName, nodeId);
        IdGeneratorOptions options = new IdGeneratorOptions(Short.parseShort(nodeId));
        options.BaseTime = startTimestamp;
        options.WorkerIdBitLength = uidProperties.getNodeIdBits();
        options.SeqBitLength = uidProperties.getSequenceBits();
        YitIdHelper.setIdGenerator(options);
    }

}