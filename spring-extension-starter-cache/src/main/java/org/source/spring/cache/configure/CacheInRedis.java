package org.source.spring.cache.configure;

import org.source.spring.cache.constant.CacheConstant;

import java.lang.annotation.*;

@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheInRedis {
    boolean enable() default true;

    long ttl() default CacheConstant.FROM_CONFIG;

    /**
     * <pre>
     * redis上缓存的单条数据的java类型
     * 当key单条时，value类型=方法返回值类型
     * 当key批量时，value类型=方法返回值容器中对象{@literal （Map<K,V>的V,List<E>,Set<E>的E）}的类型
     * </pre>
     *
     * @return classes
     */
    Class<?>[] valueClasses() default {};

    /**
     * 分片策略，默认不分片
     *
     * <p>当业务 key 数量很大时，使用 HSET 分片存储，避免 Redis key 过多影响性能</p>
     * <p>一旦配置了分片策略，所有该缓存的批量数据都会使用 HSET 分片存储</p>
     *
     * @return 分片策略
     */
    ShardStrategyEnum shardStrategy() default ShardStrategyEnum.NONE;

    /**
     * 分片参数，含义随分片策略不同而不同
     *
     * <table border="1">
     *   <tr>
     *     <th>分片策略</th>
     *     <th>shardValue 含义</th>
     *     <th>分片索引计算</th>
     *     <th>适用场景</th>
     *   </tr>
     *   <tr>
     *     <td>{@link ShardStrategyEnum#FIXED_SHARD}</td>
     *     <td>分片总数量</td>
     *     <td>{@code shardIndex = hashCode(key) % shardValue}</td>
     *     <td>key 分布均匀，如随机字符串</td>
     *   </tr>
     *   <tr>
     *     <td>{@link ShardStrategyEnum#FIXED_SIZE}</td>
     *     <td>每个分片的数据量</td>
     *     <td>{@code shardIndex = key / shardValue}</td>
     *     <td>有序自增主键，如数据库 ID</td>
     *   </tr>
     * </table>
     *
     * <h3>示例</h3>
     * <ul>
     *   <li><b>FIXED_SHARD</b>：shardValue=16 → 数据分散到 16 个 Hash（cacheName:shard:0 ~ cacheName:shard:15）</li>
     *   <li><b>FIXED_SIZE</b>：shardValue=100000 → ID 0~99999 存入 shard:1，ID 100000~199999 存入 shard:2
     *       <p>注意：FIXED_SIZE 策略要求 key 必须是 Integer 或 Long 类型</p>
     *   </li>
     * </ul>
     *
     * @return 分片参数，默认 16
     */
    int shardValue() default 16;
}
