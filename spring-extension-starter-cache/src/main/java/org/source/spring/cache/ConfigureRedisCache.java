package org.source.spring.cache;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.redisson.spring.cache.NullValue;
import org.source.spring.cache.configure.ConfigureCache;
import org.source.spring.cache.configure.ConfigureCacheProperties;
import org.source.spring.cache.configure.ReturnTypeEnum;
import org.source.spring.cache.configure.ShardStrategyEnum;
import org.source.spring.cache.pubsub.ConfigureCacheMessage;
import org.source.spring.cache.pubsub.PublishTopic;
import org.source.spring.cache.strategy.ConfigureCacheInterceptor;
import org.source.spring.cache.strategy.PartialCacheResult;
import org.source.spring.cache.strategy.PartialCacheStrategyEnum;
import org.source.spring.common.spel.SpElUtil;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Streams;
import org.springframework.data.redis.cache.ConfigureRedisCacheWriter;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.Nullable;

import java.util.*;

/**
 * @author zengfugen
 */
@Slf4j
public class ConfigureRedisCache extends RedisCache {
    private final ConfigureRedisCacheWriter cacheWriter;
    private final Map<String, ConfigureCacheProperties> configureCacheExpendMap;

    protected ConfigureRedisCache(String name,
                                  ConfigureRedisCacheWriter cacheWriter,
                                  RedisCacheConfiguration cacheConfig,
                                  Map<String, ConfigureCacheProperties> configureCacheExpendMap) {
        super(name, cacheWriter, cacheConfig);
        this.cacheWriter = cacheWriter;
        // Immutable
        this.configureCacheExpendMap = Map.copyOf(configureCacheExpendMap);
    }

    @Override
    public @Nullable ValueWrapper get(Object key) {
        ConfigureCacheProperties cacheProperties = configureCacheExpendMap.get(super.getName());
        if (Objects.isNull(cacheProperties)) {
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("ConfigureCache cacheProperties:{}", Jsons.str(cacheProperties));
        }
        Object value;
        if (key instanceof Collection<?> cs) {
            value = this.mGetCache(cs, cacheProperties);
        } else {
            value = this.getCache(key, cacheProperties);
        }
        if (log.isDebugEnabled()) {
            log.debug("ConfigureCache cache value:{}", Objects.isNull(value) ? null : Jsons.str(value));
        }
        return super.toValueWrapper(value);
    }

    protected @Nullable Object mGetCache(Collection<?> keys, ConfigureCacheProperties cacheProperties) {
        Map<Object, Object> kvMap;
        if (cacheProperties.isCacheInJvm()) {
            if (log.isDebugEnabled()) {
                log.debug("ConfigureCache mGet from jvm,key:{}", Jsons.str(keys));
            }
            kvMap = cacheProperties.getJvmCache().getAllPresent(keys);
        } else {
            kvMap = this.redisValueAsMap(keys);
        }
        return resolvePartialCache(keys, kvMap, cacheProperties);
    }

    /**
     * 批量获取缓存时，如果仅获取到部分缓存，即有部分key对应的value为null，该如何处理
     *
     * @param keys            keys
     * @param kvMap           结果kvMap
     * @param cacheProperties cacheProperties
     * @return 结果，如果{@link PartialCacheStrategyEnum#PARTIAL_TRUST}策略，返回{@link PartialCacheResult}
     * 具体逻辑见{@link ConfigureCacheInterceptor}
     * @apiNote {@link ConfigureCache#key()}没有值时，key都是方法的第一个参数。
     * 如果key有值，无法保证准确的获取到key的引用变量，以及值不会被改变，{@link ConfigureCacheInterceptor} 就没法正确处理，所以排除这种情况
     */
    protected @Nullable Object resolvePartialCache(Collection<?> keys, Map<Object, Object> kvMap, ConfigureCacheProperties cacheProperties) {
        Object result = valuesConverter(kvMap, cacheProperties.getReturnType());
        if (Objects.isNull(result) || keys.size() == kvMap.size()) {
            return result;
        }
        PartialCacheStrategyEnum partialCache = cacheProperties.getPartialCache();
        if (PartialCacheStrategyEnum.DISTRUST.equals(partialCache)) {
            return null;
        } else if (PartialCacheStrategyEnum.TRUST.equals(partialCache)) {
            return result;
        } else {
            List<Object> cachedKeys = new ArrayList<>(keys.size());
            keys.forEach(k -> {
                if (kvMap.containsKey(k)) {
                    cachedKeys.add(k);
                }
            });
            return new PartialCacheResult(result, cachedKeys);
        }
    }

    protected Map<Object, Object> redisValueAsMap(Collection<?> keys) {
        if (log.isDebugEnabled()) {
            log.debug("ConfigureCache mGet from redis,keys:{}", Jsons.str(keys));
        }
        List<?> values = this.mGetFromRedis(keys);
        LinkedHashMap<Object, Object> kv = LinkedHashMap.newLinkedHashMap(keys.size());
        Iterator<?> keyIterator = keys.iterator();
        Iterator<?> valueIterator = values.iterator();
        while (keyIterator.hasNext()) {
            Object k = keyIterator.next();
            Object v = valueIterator.next();
            if (Objects.nonNull(v)) {
                kv.put(k, v);
            }
        }
        return kv;
    }

    /**
     * 批量从 Redis 读取缓存数据
     * <p>
     * 根据分片策略决定使用普通读取还是分片读取：
     * <ul>
     *   <li>NONE 策略：使用 MGET 批量读取普通 key-value 数据</li>
     *   <li>FIXED_SHARD/FIXED_SIZE 策略：使用 HMGET 从多个 Hash 分片读取数据</li>
     * </ul>
     *
     * @param keys 要读取的缓存 key 集合
     * @return 值列表，与 keys 顺序对应，key 不存在时对应位置为 null
     */
    protected List<Object> mGetFromRedis(Collection<?> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return List.of();
        }
        ConfigureCacheProperties cacheProperties = configureCacheExpendMap.get(this.getName());
        ShardStrategyEnum shardStrategy = cacheProperties.getShardStrategy();

        if (shardStrategy == ShardStrategyEnum.NONE) {
            return this.mGetFromNormalRedis(keys);
        } else {
            return this.mGetFromShardedRedis(keys, cacheProperties);
        }
    }

    /**
     * 从普通 Redis key-value 结构批量读取数据（不分片）
     * <p>
     * 使用 Redis MGET 命令一次性获取多个 key 的值，适用于数据量较小或不需要分片的场景。
     * 每个 key 都是独立的 Redis String 类型 key。
     *
     * @param keys 要读取的缓存 key 集合
     * @return 值列表，与 keys 顺序对应，key 不存在时对应位置为 null
     */
    protected List<Object> mGetFromNormalRedis(Collection<?> keys) {
        byte[][] keyBytes = Streams.map(keys, this::createAndConvertCacheKey2).toArray(byte[][]::new);
        List<byte[]> valueByteList = cacheWriter.mGet(this.getName(), super.getCacheConfiguration().getTtl(), keyBytes);
        return Streams.map(valueByteList, this::deserializeCacheValueNullable).toList();
    }


    /**
     * 从 HSET 分片结构批量读取数据
     * <p>
     * 根据 key 计算分片索引，将 keys 按分片分组后批量读取。
     * 分片结构为多个 Hash key：{@code cacheName:shard:0}, {@code cacheName:shard:1}, ...
     * 每个 Hash 的 field 为原始 key 的字符串形式，value 为序列化的缓存值。
     *
     * <h3>分片策略说明</h3>
     * <ul>
     *   <li>FIXED_SHARD：按 hashCode 分片，{@code shardIndex = hashCode % shardValue}
     *       <p>适用于 key 分布均匀的场景，shardValue 为分片数量</p>
     *   </li>
     *   <li>FIXED_SIZE：按数值范围分片，{@code shardIndex = key / shardValue}
     *       <p>适用于有序自增主键场景，key 必须是 Integer/Long 类型</p>
     *       <p>例如：shardValue=100000，则 ID 0-99999 存入 shard:1，ID 100000-199999 存入 shard:2</p>
     *   </li>
     * </ul>
     *
     * <h3>读取流程</h3>
     * <pre>
     * 1. 按 shardIndex 分组 keys → Map&lt;shardCacheName, List&lt;fieldKey&gt;&gt;
     * 2. 调用 cacheWriter.hGetShard 批量读取各分片
     * 3. 返回值列表（与原始 keys 顺序一致）
     * </pre>
     *
     * @param keys            要读取的缓存 key 集合
     * @param cacheProperties 缓存配置，包含 shardStrategy 和 shardValue
     * @return 值列表，与 keys 顺序对应，key 不存在时对应位置为 null
     */
    protected List<Object> mGetFromShardedRedis(Collection<?> keys, ConfigureCacheProperties cacheProperties) {
        ShardStrategyEnum shardStrategy = cacheProperties.getShardStrategy();
        String cacheName = this.getName();
        // shardKeys 结构：Map<shardCacheName, List<fieldKey>>
        // shardCacheName = cacheName:shard:idx（Redis Hash key）
        // fieldKey = 原始 key 的字符串序列化（Hash field）
        Map<byte[], List<byte[]>> shardKeys = new LinkedHashMap<>();
        keys.forEach(k -> {
            int idx = shardStrategy.shardIndex(k, cacheProperties.getShardValue());
            byte[] shardCacheName = this.createShardCacheName(cacheName, idx);
            shardKeys.putIfAbsent(shardCacheName, new ArrayList<>(keys.size()));
            List<byte[]> ks = shardKeys.get(shardCacheName);
            ks.add(this.serializeCacheKey(this.convertKey(k)));
        });
        List<byte[]> values = cacheWriter.hGetShard(cacheName, super.getCacheConfiguration().getTtl(), shardKeys);
        return Streams.map(values, this::deserializeCacheValueNullable).toList();
    }

    protected @Nullable Object deserializeCacheValueNullable(@Nullable byte[] value) {
        if (Objects.isNull(value)) {
            return null;
        }
        return super.deserializeCacheValue(value);
    }

    public @Nullable Object getCache(Object key, ConfigureCacheProperties cacheProperties) {
        Object value;
        if (cacheProperties.isCacheInJvm()) {
            if (log.isDebugEnabled()) {
                log.debug("ConfigureCache get from jvm,key:{}", Jsons.str(key));
            }
            value = cacheProperties.getJvmCache().getIfPresent(key);
        } else {
            value = this.getFromRedis(key);
        }
        return value;
    }

    public @Nullable Object getFromRedis(Object key) {
        if (log.isDebugEnabled()) {
            log.debug("ConfigureCache get from redis, key:{}", Jsons.str(key));
        }
        byte[] value = cacheWriter.get(this.getName(), super.getCacheConfiguration().getTtl(), createAndConvertCacheKey2(key));
        return this.deserializeCacheValueNullable(value);
    }

    protected @Nullable Object valuesConverter(@Nullable Map<?, ?> kvMap, ReturnTypeEnum returnType) {
        if (Objects.isNull(kvMap) || kvMap.isEmpty()) {
            return null;
        }
        switch (returnType) {
            case MAP -> {
                return kvMap;
            }
            case LIST -> {
                return new ArrayList<>(kvMap.values());
            }
            case SET -> {
                return new HashSet<>(kvMap.values());
            }
            default -> {
                return null;
            }
        }
    }


    @Override
    public void put(Object key, @Nullable Object value) {
        if (Objects.isNull(value)) {
            if (super.isAllowNullValues()) {
                this.putCacheNullValue(key);
            }
            return;
        }
        this.putCache(key, value);
    }

    protected void putCache(Object key, Object value) {
        if (log.isDebugEnabled()) {
            log.debug("ConfigureCache putCache. key:{}, value:{}", Jsons.str(key), Jsons.str(value));
        }
        ConfigureCacheProperties cacheProperties = configureCacheExpendMap.get(super.getName());
        Pair<Boolean, Map<Object, Object>> pair = this.parsePutPair(value, cacheProperties);
        boolean isMulti = pair.getLeft();
        Map<?, ?> map = pair.getRight();
        if (cacheProperties.isCacheInJvm()) {
            log.debug("ConfigureCache put jvm");
            if (isMulti) {
                if (!map.isEmpty()) {
                    cacheProperties.getJvmCache().putAll(map);
                }
            } else {
                cacheProperties.getJvmCache().put(key, value);
            }
        }
        if (cacheProperties.isCacheInRedis()) {
            log.debug("ConfigureCache put redis");
            String cacheName = this.getName();
            if (isMulti) {
                if (!map.isEmpty()) {
                    this.mPut(cacheName, map);
                }
            } else {
                this.put(cacheName, key, value);
            }
        }
    }

    protected void putCacheNullValue(Object key) {
        ConfigureCacheProperties cacheProperties = configureCacheExpendMap.get(super.getName());
        if (cacheProperties.isCacheInJvm()) {
            log.debug("ConfigureCache put jvm, value is null");
            if (key instanceof Collection<?> cs) {
                Map<Object, Object> map = new HashMap<>(cs.size());
                cs.forEach(c -> map.put(c, null));
                cacheProperties.getJvmCache().putAll(map);
            } else {
                cacheProperties.getJvmCache().put(key, null);
            }
        }
        if (cacheProperties.isCacheInRedis()) {
            log.debug("ConfigureCache put redis, value is null");
            String cacheName = this.getName();
            if (key instanceof Collection<?> cs) {
                Map<Object, Object> map = new HashMap<>(cs.size());
                cs.forEach(c -> map.put(c, NullValue.INSTANCE));
                this.mPut(cacheName, map);
            } else {
                this.put(cacheName, key, NullValue.INSTANCE);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected Pair<Boolean, Map<Object, Object>> parsePutPair(Object value, ConfigureCacheProperties cacheProperties) {
        ReturnTypeEnum returnType = cacheProperties.getReturnType();
        if (value instanceof Map<?, ?> map && ReturnTypeEnum.MAP.equals(returnType)) {
            return Pair.of(true, (Map<Object, Object>) map);
        } else if (value instanceof Collection<?> cs
                && (ReturnTypeEnum.LIST.equals(returnType) || ReturnTypeEnum.SET.equals(returnType))) {
            return Pair.of(true, this.parsePutPairWhenCollection(cs, cacheProperties));
        }
        return Pair.of(false, null);
    }

    protected Map<Object, Object> parsePutPairWhenCollection(Collection<?> values, ConfigureCacheProperties cacheProperties) {
        return Streams.toMap(values, v -> {
            String keySpEl = cacheProperties.getCacheKeySpEl();
            BaseExceptionEnum.NOT_EMPTY.notEmpty(keySpEl, "方法返回为集合情况下，cacheKeySpEl必填。cacheName:{}", super.getName());
            // 方法返回为集合情况下,需要填写key的class类型
            Object parsedKey = SpElUtil.parseSpEl(keySpEl, v, cacheProperties.getJvmKeyClass());
            BaseExceptionEnum.NOT_NULL.nonNull(parsedKey, "方法返回为集合情况下，cacheKeySpEl解析结果为null. cacheName:{}", super.getName());
            return parsedKey;
        }, v -> v);
    }

    public void put(String cacheName, Object key, @Nullable Object value) {
        Object cacheValue = value;
        if (Objects.isNull(cacheValue)) {
            if (isAllowNullValues()) {
                cacheValue = NullValue.INSTANCE;
            } else {
                throw BaseExceptionEnum.NOT_NULL.newException("cache:{}的值不允许为null", cacheName);
            }
        }
        cacheWriter.put(cacheName, this.createAndConvertCacheKey2(key), serializeCacheValue(cacheValue), super.getCacheConfiguration().getTtl());
    }

    /**
     * 批量写入缓存到 Redis
     * <p>
     * 根据分片策略决定使用普通写入还是分片写入：
     * <ul>
     *   <li>NONE 策略：使用 MSET 批量写入普通 key-value 数据</li>
     *   <li>FIXED_SHARD/FIXED_SIZE 策略：使用 HMSET 写入多个 Hash 分片</li>
     * </ul>
     *
     * @param cacheName 缓存名称
     * @param map       key-value 映射，key 为缓存键，value 为缓存值
     */
    protected void mPut(String cacheName, Map<?, ?> map) {
        ConfigureCacheProperties cacheProperties = configureCacheExpendMap.get(super.getName());
        ShardStrategyEnum shardStrategy = cacheProperties.getShardStrategy();
        if (ShardStrategyEnum.NONE.equals(shardStrategy)) {
            this.mPutNoShards(cacheName, map);
        } else {
            this.mPutShards(cacheName, map, shardStrategy, cacheProperties);
        }
    }

    /**
     * 批量写入缓存到普通 Redis key-value 结构（不分片）
     * <p>
     * 使用 Redis MSET 命令一次性写入多个 key-value，适用于数据量较小或不需要分片的场景。
     * 每个 key 都是独立的 Redis String 类型 key。
     *
     * @param cacheName 缓存名称
     * @param map       key-value 映射，key 为缓存键，value 为缓存值
     */
    protected void mPutNoShards(String cacheName, Map<?, ?> map) {
        Map<byte[], byte[]> tuple = HashMap.newHashMap(map.size());
        map.forEach((k, v) -> {
            if (Objects.nonNull(v) || isAllowNullValues()) {
                tuple.put(this.createAndConvertCacheKey2(k), super.serializeCacheValue(v));
            }
        });
        cacheWriter.mPut(cacheName, tuple, super.getCacheConfiguration().getTtl());
    }

    /**
     * 批量写入缓存到 HSET 分片结构
     * <p>
     * 根据 key 计算分片索引，将数据按分片分组后批量写入。
     * 分片结构为多个 Hash key：{@code cacheName:shard:0}, {@code cacheName:shard:1}, ...
     * 每个 Hash 的 field 为原始 key 的字符串形式，value 为序列化的缓存值。
     *
     * <h3>分片策略说明</h3>
     * <ul>
     *   <li>FIXED_SHARD：按 hashCode 分片，{@code shardIndex = hashCode % shardCount}
     *       <p>适用于 key 分布均匀的场景，如随机字符串 key</p>
     *   </li>
     *   <li>FIXED_SIZE：按数值范围分片，{@code shardIndex = key / shardValue}
     *       <p>适用于有序自增主键场景，如数据库自增 ID，每 shardValue 条数据为一个分片</p>
     *       <p>例如：shardValue=100000，则 ID 0-99999 存入 shard:0，ID 100000-199999 存入 shard:1</p>
     *   </li>
     * </ul>
     *
     * <h3>数据结构</h3>
     * <pre>
     * Redis Key: cacheName:shard:0
     * Hash Fields:
     *   "key1" → serializedValue1
     *   "key2" → serializedValue2
     *
     * Redis Key: cacheName:shard:1
     * Hash Fields:
     *   "key100000" → serializedValue100000
     * </pre>
     *
     * @param cacheName       缓存名称
     * @param map             key-value 映射
     * @param shardStrategy   分片策略
     * @param cacheProperties 缓存配置，包含 shardValue（FIXED_SHARD 为分片数量，FIXED_SIZE 为分片区间大小）
     */
    protected void mPutShards(String cacheName, Map<?, ?> map, ShardStrategyEnum shardStrategy,
                              ConfigureCacheProperties cacheProperties) {
        // shardMap 结构：Map<shardCacheName, Map<fieldKey, value>>
        // shardCacheName = cacheName:shard:idx
        // fieldKey = 原始 key 的字符串序列化
        // value = 缓存值的序列化
        Map<byte[], Map<byte[], byte[]>> shardMap = HashMap.newHashMap(32);
        map.forEach((k, v) -> {
            if (Objects.isNull(v) && !isAllowNullValues()) {
                return;
            }
            int idx = shardStrategy.shardIndex(k, cacheProperties.getShardValue());
            byte[] shardCacheName = this.createShardCacheName(cacheName, idx);
            shardMap.putIfAbsent(shardCacheName, HashMap.newHashMap(map.size()));
            Map<byte[], byte[]> shardData = shardMap.get(shardCacheName);
            shardData.put(this.serializeCacheKey(this.convertKey(k)), super.serializeCacheValue(v));
        });
        cacheWriter.hPutShards(cacheName, shardMap, super.getCacheConfiguration().getTtl());
    }

    @Override
    public void evict(Object key) {
        ConfigureCacheProperties cacheProperties = this.configureCacheExpendMap.get(super.getName());
        String cacheName = this.getName();
        if (log.isDebugEnabled()) {
            log.debug("ConfigureCache evict jvm, cacheName:{}, key:{}", cacheName, Jsons.str(key));
        }
        if (cacheProperties.isCacheInJvm()) {
            log.debug("ConfigureCache evict jvm");
            cacheProperties.getJvmCache().invalidate(key);
            byte[] channelTopic = RedisSerializer.string().serialize(PublishTopic.TOPIC_CONFIGURE_CACHE);
            byte[] message = RedisSerializer.string().serialize(Jsons.str(new ConfigureCacheMessage(cacheName, key instanceof Collection<?>, Jsons.str(key))));
            if (Objects.nonNull(channelTopic) && Objects.nonNull(message)) {
                try {
                    Long count = cacheWriter.publish(cacheName, channelTopic, message);
                    log.debug("ConfigureCache publish evict message, receive clients:{}", count);
                } catch (Exception e) {
                    log.error("may not connect redis, {}", e.getMessage());
                }
            }
        }
        if (cacheProperties.isCacheInRedis()) {
            log.debug("ConfigureCache evict redis");
            if (key instanceof Collection<?> cs) {
                byte[][] keyBytes = Streams.map(cs, this::createAndConvertCacheKey2).toArray(byte[][]::new);
                cacheWriter.mRemove(cacheName, keyBytes);
            } else {
                cacheWriter.remove(cacheName, createAndConvertCacheKey2(key));
            }
        }
    }

    @Override
    public void clear() {
        ConfigureCacheProperties cacheProperties = this.configureCacheExpendMap.get(super.getName());
        String cacheName = this.getName();
        if (log.isDebugEnabled()) {
            log.debug("ConfigureCache clear jvm, cacheName:{}", cacheName);
        }
        if (cacheProperties.isCacheInJvm()) {
            cacheProperties.getJvmCache().invalidateAll();
        }
        if (cacheProperties.isCacheInRedis()) {
            super.clear();
        }
    }

    protected byte[] createAndConvertCacheKey2(Object key) {
        return serializeCacheKey(createCacheKey(key));
    }

    protected byte[] createShardCacheName(String cacheName, int idx) {
        return this.createAndConvertCacheKey2(cacheName + ":shard:" + idx);
    }
}
