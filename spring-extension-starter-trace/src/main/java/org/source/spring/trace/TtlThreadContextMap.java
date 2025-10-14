package org.source.spring.trace;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.apache.logging.log4j.spi.ThreadContextMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class TtlThreadContextMap implements ThreadContextMap {
    private final ThreadLocal<Map<String, String>> localMap = new TransmittableThreadLocal<>();

    @Override
    public void clear() {
        this.getOrDefault().clear();
        localMap.remove();
    }

    @Override
    public boolean containsKey(String key) {
        return this.getOrDefault().containsKey(key);
    }

    @Override
    public String get(String key) {
        return this.getOrDefault().get(key);
    }

    @Override
    public Map<String, String> getCopy() {
        return new HashMap<>(this.getOrDefault());
    }

    @Override
    public Map<String, String> getImmutableMapOrNull() {
        return this.getOrDefault();
    }

    @Override
    public boolean isEmpty() {
        return this.getOrDefault().isEmpty();
    }

    @Override
    public void put(String key, String value) {
        this.getOrDefault().put(key, value);
    }

    @Override
    public void remove(String key) {
        this.getOrDefault().remove(key);
    }

    public Map<String, String> getOrDefault() {
        Map<String, String> stringMap = localMap.get();
        if (Objects.nonNull(stringMap)) {
            return stringMap;
        } else {
            localMap.set(new ConcurrentHashMap<>(16));
            return localMap.get();
        }
    }
}
