package org.source.spring.log;

import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@UtilityClass
public class LogContext {
    private static final ThreadLocal<Map<String, Deque<Map<String, Object>>>> VARIABLES =
            TransmittableThreadLocal.withInitial(ConcurrentHashMap::new);

    private static Deque<Map<String, Object>> getDeque(String scopeName) {
        Map<String, Deque<Map<String, Object>>> nameDequeMap = VARIABLES.get();
        return nameDequeMap.computeIfAbsent(scopeName, k -> new ArrayDeque<>());
    }

    private static Map<String, Object> getData(String scopeName) {
        Deque<Map<String, Object>> deque = getDeque(scopeName);
        Map<String, Object> variables = deque.peekFirst();
        if (Objects.isNull(variables)) {
            variables = new ConcurrentHashMap<>(16);
            deque.addFirst(variables);
        }
        return variables;
    }

    public static void clear(String scopeName) {
        getDeque(scopeName).remove();
    }

    /**
     * 获取当前层级指定域中的变量的值
     *
     * @param scopeName 域名称
     * @param key       变量名
     * @param <E>       泛型
     * @return 变量的值
     */
    @SuppressWarnings("unchecked")
    public static <E> @Nullable E get(String scopeName, String key) {
        return (E) getData(scopeName).get(key);
    }

    public static void set(String scopeName, String k, @Nullable Object v) {
        Map<String, Object> map = getData(scopeName);
        if (Objects.nonNull(v)) {
            map.put(k, v);
        } else {
            map.remove(k);
        }
    }

    public static void init(String scopeName) {
        getDeque(scopeName).push(HashMap.newHashMap(16));
    }

    static void clear() {
        VARIABLES.remove();
    }

    /**
     * 获取所有层级指定域中的变量的值
     *
     * @param scopeName 域名称
     * @param key       变量名
     * @param <E>       泛型
     * @return 变量的值
     */
    @SuppressWarnings("unchecked")
    public static <E> @Nullable E find(String scopeName, String key) {
        Deque<Map<String, Object>> deque = getDeque(scopeName);
        Map<String, Object> maps = new ArrayList<>(deque).stream().reduce(HashMap.newHashMap(4),
                (m1, m2) -> {
                    m1.putAll(m2);
                    return m1;
                });
        return (E) maps.get(key);
    }
}