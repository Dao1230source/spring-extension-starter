package org.source.spring.log;

import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@UtilityClass
public class LogContext {
    private static final ThreadLocal<Map<String, Deque<Map<String, Object>>>> VARIABLES =
            TransmittableThreadLocal.withInitial(ConcurrentHashMap::new);

    @NotNull
    private static Deque<Map<String, Object>> getDeque(String scopeName) {
        Map<String, Deque<Map<String, Object>>> nameDequeMap = VARIABLES.get();
        return nameDequeMap.computeIfAbsent(scopeName, k -> new ArrayDeque<>());
    }

    @NotNull
    private static Map<String, Object> getData(String scopeName) {
        Deque<Map<String, Object>> deque = getDeque(scopeName);
        Map<String, Object> variables = deque.peekFirst();
        if (Objects.isNull(variables)) {
            variables = new ConcurrentHashMap<>(16);
            deque.addFirst(variables);
        }
        return variables;
    }

    public static void remove(String scopeName) {
        getDeque(scopeName).remove();
    }

    public static Object get(String scopeName, String key) {
        return getData(scopeName).get(key);
    }

    public static void set(String scopeName, String k, Object v) {
        Map<String, Object> map = getData(scopeName);
        if (Objects.nonNull(v)) {
            map.put(k, v);
        } else {
            map.remove(k);
        }
    }

    static void putEmpty(String scopeName) {
        getDeque(scopeName).push(HashMap.newHashMap(16));
    }

    static void clear() {
        VARIABLES.remove();
    }

    static Object searchAll(String scopeName, String k) {
        Deque<Map<String, Object>> deque = getDeque(scopeName);
        Map<String, Object> maps = new ArrayList<>(deque).stream().reduce(HashMap.newHashMap(4), (m1, m2) -> {
            m1.putAll(m2);
            return m1;
        });
        return maps.get(k);
    }
}
