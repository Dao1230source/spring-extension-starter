package org.source.spring.cache.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.source.utility.exceptions.BaseException;
import org.source.utility.exceptions.EnumProcessor;

/**
 * spring cache exception
 */
@Getter
@AllArgsConstructor
public enum CacheExceptionEnum implements EnumProcessor<BaseException> {
    /**
     * spring cache
     */
    PARTIAL_CACHE_EXCEPTION("spring-cache处理部分缓存时异常，可能存在分布式缓存不一致问题"),
    CACHE_MANAGER_TYPE_ERROR("spring cache CacheManager必须是ConfigureRedisCacheManager类型"),
    CANNOT_SET_STRATEGY_AS_PARTIAL_TRUST("不能将partialCacheStrategy设置为PARTIAL_TRUST"),
    ;

    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
