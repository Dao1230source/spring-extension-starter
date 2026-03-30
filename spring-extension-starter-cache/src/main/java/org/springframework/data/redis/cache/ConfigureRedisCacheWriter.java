package org.springframework.data.redis.cache;

import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisHashCommands;
import org.springframework.lang.NonNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 需要扩展 mGet 等方法，但 {@see org.springframework.data.redis.cache.DefaultRedisCacheWriter} 无法继承、新建，只好复制过来
 *
 * @author zengfugen
 */
public class ConfigureRedisCacheWriter extends DefaultRedisCacheWriter {
    private final RedisConnectionFactory connectionFactory;
    private final Duration sleepTime;
    private final CacheStatisticsCollector statistics;

    /**
     * @param connectionFactory must not be {@literal null}.
     */
    public ConfigureRedisCacheWriter(RedisConnectionFactory connectionFactory) {
        this(connectionFactory, Duration.ZERO, CacheStatisticsCollector.none(), BatchStrategies.scan(1000));
    }


    public ConfigureRedisCacheWriter(@NonNull RedisConnectionFactory connectionFactory, @NonNull Duration sleepTime,
                                     @NonNull CacheStatisticsCollector cacheStatisticsCollector, @NonNull BatchStrategy batchStrategy) {
        super(connectionFactory, sleepTime, cacheStatisticsCollector, batchStrategy);
        this.connectionFactory = connectionFactory;
        this.sleepTime = sleepTime;
        this.statistics = cacheStatisticsCollector;
    }

    public byte[] get(@NonNull String name, @NonNull Duration ttl, byte[] key) {
        return executeSuper(name, connection -> {
            byte[] bytes = connection.stringCommands().get(key);
            if (bytes != null) {
                statistics.incHits(name);
                connection.keyCommands().expire(key, ttl.getSeconds());
            } else {
                statistics.incMisses(name);
            }
            return bytes;
        });
    }


    public List<byte[]> mGet(@NonNull String name, @NonNull Duration ttl, byte[]... keys) {
        return executeSuper(name, connection -> {
            List<byte[]> bytes = connection.stringCommands().mGet(keys);
            if (null != bytes) {
                for (int i = 0; i < keys.length; i++) {
                    byte[] key = keys[i];
                    byte[] value = bytes.get(i);
                    if (value != null) {
                        statistics.incHits(name);
                        connection.keyCommands().expire(key, ttl.getSeconds());
                    } else {
                        statistics.incMisses(name);
                    }
                }
            }
            return bytes;
        });
    }

    /**
     * 从多个 HSET 分片批量读取数据
     * <p>
     * 遍历每个分片 key，使用 HMGET 批量读取该分片的所有 field 值。
     * 分片结构：每个 shardKey 是一个 Redis Hash key，ks 是该 Hash 中的 field 列表。
     *
     * <h3>数据结构示例</h3>
     * <pre>
     * shardKeys = {
     *   "cacheName:shard:1" → ["field1", "field2", "field3"],
     *   "cacheName:shard:2" → ["field100", "field101"]
     * }
     *
     * HMGET cacheName:shard:1 field1 field2 field3 → [value1, value2, value3]
     * HMGET cacheName:shard:2 field100 field101 → [value100, value101]
     *
     * 返回：[value1, value2, value3, value100, value101]（按 shardKeys 遍历顺序）
     * </pre>
     *
     * @param name      缓存名称（用于统计）
     * @param ttl       过期时间，读取时刷新分片 key 的 TTL
     * @param shardKeys 分片映射：{@literal Map<shardCacheName, List<fieldKey>>}，有序map
     * @return 所有 field 的值列表，按 shardKeys 遍历顺序合并
     */
    public List<byte[]> hGetShard(@NonNull String name, @NonNull Duration ttl,
                                  Map<byte[], List<byte[]>> shardKeys) {
        return this.executeSuper(name, connection -> {
            RedisHashCommands hashCommands = connection.hashCommands();
            List<byte[]> results = new ArrayList<>();
            shardKeys.forEach((shardKey, ks) -> {
                byte[][] keys = ks.toArray(new byte[ks.size()][]);
                List<byte[]> values = hashCommands.hMGet(shardKey, keys);
                if (Objects.nonNull(values)) {
                    statistics.incHits(name);
                    connection.keyCommands().expire(shardKey, ttl.getSeconds());
                    results.addAll(values);
                } else {
                    statistics.incMisses(name);
                }
            });
            return results;
        });
    }

    public void mPut(@NonNull String name, @NonNull Map<byte[], byte[]> tuple, @NonNull Duration ttl) {
        this.executeSuper(name, connection -> {
            Boolean successful = connection.stringCommands().mSet(tuple);
            if (Boolean.TRUE.equals(successful) && ttl.isPositive()) {
                tuple.keySet().forEach(k -> {
                    connection.keyCommands().expire(k, ttl.toSeconds());
                    statistics.incPuts(name);
                });
            }
            return "OK";
        });
    }

    /**
     * 使用 HSET 分片存储多分片数据
     *
     * @param name 缓存名称
     * @param ttl  过期时间
     */
    public void hPutShards(@NonNull String name, Map<byte[], Map<byte[], byte[]>> shardMap, @NonNull Duration ttl) {
        this.executeSuper(name, connection -> {
            RedisHashCommands hashCommands = connection.hashCommands();
            shardMap.forEach((key, kv) -> {
                // 使用 hMSet 批量写入 Hash
                hashCommands.hMSet(key, kv);
                // 设置过期时间
                if (ttl.isPositive()) {
                    connection.keyCommands().expire(key, ttl.toSeconds());
                }
                statistics.incPuts(name);
            });
            return "OK";
        });
    }

    public void mRemove(@NonNull String name, byte[]... keys) {
        executeSuper(name, connection -> connection.keyCommands().del(keys));
    }

    protected <T> T executeSuper(String name, Function<RedisConnection, T> callback) {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            checkAndWaitUnlocked(name, connection);
            return callback.apply(connection);
        }
    }

    protected void checkAndWaitUnlocked(String name, RedisConnection connection) {
        if (!isLocking()) {
            return;
        }
        try {
            while (doCheckLock(name, connection)) {
                Thread.sleep(sleepTime.toMillis());
            }
        } catch (InterruptedException ex) {
            // Re-interrupt current thread, to allow other participants to react.
            Thread.currentThread().interrupt();
            throw new PessimisticLockingFailureException(String.format("Interrupted while waiting to unlock cache %s", name), ex);
        }
    }

    protected boolean isLocking() {
        return sleepTime.isPositive();
    }

    public Long publish(String name, byte[] channel, byte[] message) {
        return executeSuper(name, connection -> connection.commands().publish(channel, message));
    }
}
