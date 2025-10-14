package org.source.spring.trace;

import lombok.experimental.UtilityClass;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.function.Supplier;

@UtilityClass
public class TraceContext {

    public static final String TRACE_ID = "TRACE_ID";
    public static final String USER_ID = "USER_ID";
    public static final String SPACE_ID = "SPACE_ID";
    /**
     * 系统密钥，由gateway模块配置并添加到request header中，其他所有模块的请求如果没有带有该key，视为非法请求
     */
    public static final String SECRET_KEY = "SECRET_KEY";
    public static final String EXTENSION_DATA_PREFIX = "E-";
    public static final String DEFAULT_USER_ID = "system";
    public static final String DEFAULT_SPACE_ID = "public";

    public static String getTraceId() {
        return get(TRACE_ID);
    }

    public static String getUserId() {
        return get(USER_ID);
    }

    public static String getUserIdOrDefault() {
        return compute(USER_ID, () -> DEFAULT_USER_ID);
    }

    public static String getSpaceIdOrDefault() {
        return compute(SPACE_ID, () -> DEFAULT_SPACE_ID);
    }

    public static Map<String, String> extensionData() {
        Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();
        copyOfContextMap.remove(TRACE_ID);
        copyOfContextMap.remove(USER_ID);
        return copyOfContextMap;
    }

    public static void put(String key, String value) {
        MDC.put(key, value);
    }

    public static String get(String key) {
        return MDC.get(key);
    }

    public static void putIfAbsent(String key, Supplier<String> preferredValue, Supplier<String> defaultValue) {
        String value = preferredValue.get();
        if (StringUtils.hasText(value)) {
            put(key, value);
            return;
        }
        compute(key, defaultValue);
    }

    public static String compute(String key, Supplier<String> defaultValue) {
        String value = get(key);
        if (StringUtils.hasText(value)) {
            return value;
        }
        value = defaultValue.get();
        put(key, value);
        return value;
    }

    public static void clear() {
        MDC.clear();
    }

}
