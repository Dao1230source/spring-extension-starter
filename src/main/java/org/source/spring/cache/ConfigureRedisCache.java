package org.source.spring.cache;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.redisson.spring.cache.NullValue;
import org.source.spring.cache.configure.ConfigureCache;
import org.source.spring.cache.configure.ConfigureCacheProperties;
import org.source.spring.cache.configure.ReturnTypeEnum;
import org.source.spring.cache.pubsub.ConfigureCacheMessage;
import org.source.spring.cache.pubsub.PublishTopic;
import org.source.spring.cache.strategy.ConfigureCacheInterceptor;
import org.source.spring.cache.strategy.PartialCacheResult;
import org.source.spring.cache.strategy.PartialCacheStrategyEnum;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.spring.SpElUtil;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Streams;
import org.springframework.data.redis.cache.ConfigureRedisCacheWriter;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.*;

/**
 * @author zengfugen
 */
@Slf4j
public class ConfigureRedisCache extends RedisCache {
    private static final String AVAILABLE_NAME = "P";
    private final ConfigureRedisCacheWriter cacheWriter;
    private final Map<String, ConfigureCacheProperties> configureCacheExpendMap;

    protected ConfigureRedisCache(@NonNull String name,
                                  @NonNull ConfigureRedisCacheWriter cacheWriter,
                                  @NonNull RedisCacheConfiguration cacheConfig,
                                  @NonNull Map<String, ConfigureCacheProperties> configureCacheExpendMap) {
        super(name, cacheWriter, cacheConfig);
        this.cacheWriter = cacheWriter;
        // Immutable
        this.configureCacheExpendMap = Map.copyOf(configureCacheExpendMap);
    }

    @Override
    public ValueWrapper get(@NonNull Object key) {
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
            log.debug("ConfigureCache cache value:{}", Jsons.str(value));
        }
        return super.toValueWrapper(value);
    }

    protected Object mGetCache(Collection<?> keys, ConfigureCacheProperties cacheProperties) {
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
    protected Object resolvePartialCache(Collection<?> keys, Map<Object, Object> kvMap, ConfigureCacheProperties cacheProperties) {
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

    protected List<Object> mGetFromRedis(Collection<?> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return List.of();
        }
        byte[][] keyBytes = Streams.map(keys, this::createAndConvertCacheKey2).toArray(byte[][]::new);
        List<byte[]> valueByteList = cacheWriter.mGet(this.getName(), super.getCacheConfiguration().getTtl(), keyBytes);
        return Streams.map(valueByteList, this::deserializeCacheValueNullable).toList();
    }

    protected Object deserializeCacheValueNullable(byte[] value) {
        if (Objects.isNull(value)) {
            return null;
        }
        return super.deserializeCacheValue(value);
    }

    public Object getCache(Object key, ConfigureCacheProperties cacheProperties) {
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

    public Object getFromRedis(Object key) {
        if (log.isDebugEnabled()) {
            log.debug("ConfigureCache get from redis, key:{}", Jsons.str(key));
        }
        byte[] value = cacheWriter.get(this.getName(), super.getCacheConfiguration().getTtl(), createAndConvertCacheKey2(key));
        return this.deserializeCacheValueNullable(value);
    }

    protected Object valuesConverter(Map<?, ?> kvMap, ReturnTypeEnum returnType) {
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
    public void put(@NonNull Object key, Object value) {
        if (Objects.isNull(value) && !super.isAllowNullValues()) {
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
            Object parsedKey = parseSpEl(keySpEl, v, cacheProperties.getJvmKeyClass());
            BaseExceptionEnum.NOT_NULL.nonNull(parsedKey, "方法返回为集合情况下，cacheKeySpEl解析结果为null. cacheName:{}", super.getName());
            return parsedKey;
        }, v -> v);
    }

    public void put(String cacheName, @NonNull Object key, @Nullable Object value) {
        Object cacheValue = value;
        if (Objects.isNull(cacheValue)) {
            if (isAllowNullValues()) {
                cacheValue = NullValue.INSTANCE;
            } else {
                throw BaseExceptionEnum.NOT_NULL.except("cache:{}的值不允许为null", cacheName);
            }
        }
        cacheWriter.put(cacheName, this.createAndConvertCacheKey2(key), serializeCacheValue(cacheValue), super.getCacheConfiguration().getTtl());
    }

    protected void mPut(String cacheName, Map<?, ?> map) {
        Map<byte[], byte[]> tuple = HashMap.newHashMap(map.size());
        map.forEach((k, v) -> {
            if (Objects.nonNull(v) || isAllowNullValues()) {
                tuple.put(this.createAndConvertCacheKey2(k), super.serializeCacheValue(v));
            }
        });
        cacheWriter.mPut(cacheName, tuple, super.getCacheConfiguration().getTtl());
    }

    @Override
    public void evict(@NonNull Object key) {
        ConfigureCacheProperties cacheProperties = this.configureCacheExpendMap.get(super.getName());
        String cacheName = this.getName();
        if (log.isDebugEnabled()) {
            log.debug("ConfigureCache evict jvm, cacheName:{}, key:{}", cacheName, Jsons.str(key));
        }
        if (cacheProperties.isCacheInJvm()) {
            log.debug("ConfigureCache evict jvm");
            byte[] channelTopic = RedisSerializer.string().serialize(PublishTopic.TOPIC_CONFIGURE_CACHE);
            byte[] message = RedisSerializer.string().serialize(Jsons.str(new ConfigureCacheMessage(cacheName, key instanceof Collection<?>, Jsons.str(key))));
            if (Objects.nonNull(channelTopic) && Objects.nonNull(message)) {
                Long count = cacheWriter.publish(cacheName, channelTopic, message);
                log.debug("ConfigureCache publish evict message, receive clients:{}", count);
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

    protected <T> @Nullable T parseSpEl(String keySpEl, Object value, Class<T> resultClass) {
        return SpElUtil.parse(keySpEl, resultClass, context -> context.setVariable(AVAILABLE_NAME, value));
    }

    protected byte[] createAndConvertCacheKey2(Object key) {
        return serializeCacheKey(createCacheKey(key));
    }
}
