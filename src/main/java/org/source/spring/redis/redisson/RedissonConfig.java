package org.source.spring.redis.redisson;

import lombok.AllArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import java.util.List;

@ConditionalOnProperty(prefix = "org.source.spring.enabled", name = "redisson", havingValue = "true")
@AllArgsConstructor
@AutoConfiguration
public class RedissonConfig {

    @ConditionalOnBean(RedisTemplate.class)
    @Bean
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        Config config = new Config();
        if (redisProperties.getCluster() != null) {
            // 集群模式配置
            ClusterServersConfig clusterServersConfig = config.useClusterServers();
            List<String> nodes = redisProperties.getCluster().getNodes();
            clusterServersConfig.addNodeAddress(nodes.stream().map(node -> "redis://" + node).toArray(String[]::new));
            if (StringUtils.hasText(redisProperties.getPassword())) {
                clusterServersConfig.setPassword(redisProperties.getPassword());
            }
        } else {
            // 单节点配置
            String address = "redis://" + redisProperties.getHost() + ":" + redisProperties.getPort();
            SingleServerConfig serverConfig = config.useSingleServer();
            serverConfig.setAddress(address);
            if (StringUtils.hasText(redisProperties.getPassword())) {
                serverConfig.setPassword(redisProperties.getPassword());
            }
            serverConfig.setDatabase(redisProperties.getDatabase());
        }
        return Redisson.create(config);
    }
}
