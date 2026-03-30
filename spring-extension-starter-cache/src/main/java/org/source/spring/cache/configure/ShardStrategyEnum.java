package org.source.spring.cache.configure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.source.spring.common.exception.SpExtException;
import org.source.spring.common.exception.SpExtExceptionEnum;

/**
 * 分片策略枚举
 *
 * <p>当业务 key 数量很大时，使用 HSET 分片存储，避免 Redis key 过多影响性能。</p>
 *
 * <h3>分片策略说明</h3>
 * <ul>
 *   <li>NONE - 不分片，使用默认的 mSet 存储</li>
 *   <li>FIXED_SHARD - 按固定分片数量分片</li>
 *   <li>FIXED_SIZE - 按固定数量分片</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
public enum ShardStrategyEnum {

    /**
     * 不分片，使用默认的 mSet 存储
     */
    NONE {
        @Override
        public int shardIndex(Object key, int shardValue) {
            return -1;
        }
    },

    /**
     * 按固定分片数量分片
     * <p>例如：shardCount=16，则将数据分散到 16 个 Hash key 中</p>
     */
    FIXED_SHARD {
        @Override
        public int shardIndex(Object key, int shardValue) {
            return Math.abs(key.hashCode()) % shardValue;
        }
    },

    /**
     * 按固定数量分片
     * <p>例如：shardValue=100000，则每 100000 条数据存一个 Hash key</p>
     */
    FIXED_SIZE {
        @Override
        public int shardIndex(Object key, int shardValue) {
            if (key instanceof Integer k) {
                return Math.abs(k) / shardValue;
            } else if (key instanceof Long l) {
                return Math.toIntExact(Math.abs(l) / shardValue);
            } else {
                try {
                    long l = Long.parseLong(key.toString());
                    return Math.toIntExact(Math.abs(l) / shardValue);
                } catch (NumberFormatException e) {
                    throw SpExtExceptionEnum.REDIS_SHARD_SIZE_KEY_MUST_INT_OR_LONG.newException();
                }
            }
        }
    };

    /**
     * 计算数据应存放的分片索引
     * <p>
     * 根据分片策略和 key 值计算该 key 应该存放在哪个分片中。
     *
     * <h3>各策略计算方式</h3>
     * <ul>
     *   <li>NONE：返回 -1，表示不分片</li>
     *   <li>FIXED_SHARD：{@code shardIndex = Math.abs(key.hashCode()) % shardValue}
     *       <p>shardValue 为分片数量，适用于随机分布的 key</p>
     *   </li>
     *   <li>FIXED_SIZE：{@code shardIndex = Math.abs(key) / shardValue}
     *       <p>shardValue 为每个分片的数据量，适用于有序自增主键</p>
     *       <p>注意：key 必须是 Integer 或 Long 类型，否则抛出异常</p>
     *   </li>
     * </ul>
     *
     * @param key        缓存 key，FIXED_SIZE 策略必须是 Integer/Long
     * @param shardValue 分片参数（FIXED_SHARD 为分片数量，FIXED_SIZE 为分片大小）
     * @return 分片索引（从 0 或 1 开始，取决于策略），NONE 返回 -1
     * @throws SpExtException 当 FIXED_SIZE 策略的 key 不是 Integer/Long 时 抛出{@link SpExtExceptionEnum#REDIS_SHARD_SIZE_KEY_MUST_INT_OR_LONG}
     *
     */
    public abstract int shardIndex(Object key, int shardValue);

}