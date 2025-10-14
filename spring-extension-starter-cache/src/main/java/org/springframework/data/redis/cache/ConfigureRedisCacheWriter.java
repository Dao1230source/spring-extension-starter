package org.springframework.data.redis.cache;

import org.jetbrains.annotations.NotNull;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
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


    public ConfigureRedisCacheWriter(@NotNull RedisConnectionFactory connectionFactory, @NotNull Duration sleepTime,
                                     @NotNull CacheStatisticsCollector cacheStatisticsCollector, @NotNull BatchStrategy batchStrategy) {
        super(connectionFactory, sleepTime, cacheStatisticsCollector, batchStrategy);
        this.connectionFactory = connectionFactory;
        this.sleepTime = sleepTime;
        this.statistics = cacheStatisticsCollector;
    }

    public byte[] get(@NotNull String name, @NotNull Duration ttl, byte @NotNull [] key) {
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


    public List<byte[]> mGet(@NotNull String name, @NotNull Duration ttl, byte[] @NotNull ... keys) {
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

    public void mPut(@NotNull String name, @NotNull Map<byte[], byte[]> tuple, @NotNull Duration ttl) {
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

    public void mRemove(@NotNull String name, byte @NotNull []... keys) {
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
