package org.source.spring.cache.strategy;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.source.spring.common.exception.SpExtExceptionEnum;
import org.source.utility.utils.Jsons;
import org.springframework.cache.interceptor.CacheInterceptor;
import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class ConfigureCacheInterceptor extends CacheInterceptor {

    @Override
    public Object invoke(@NonNull MethodInvocation invocation) throws Throwable {
        Object result = super.invoke(invocation);
        if (result instanceof PartialCacheResult partialCacheResult) {
            return this.handleWhenGetPartialCache(invocation, partialCacheResult);
        }
        return result;
    }

    /**
     * 当配置了部分信任缓存策略时，排除已缓存的key，重新执行 获取缓存（全部=null）-> 执行方法 -> 保存缓存 -> 返回结果
     *
     * @param invocation         invocation
     * @param partialCacheResult partialCacheResult
     * @return 完整的结果
     * @throws Throwable Throwable
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Object handleWhenGetPartialCache(@NonNull MethodInvocation invocation, PartialCacheResult partialCacheResult) throws Throwable {
        Object result = partialCacheResult.getResult();
        Object arg = invocation.getArguments()[0];
        if (arg instanceof Collection<?> cs) {
            List<Object> cachedKeys = partialCacheResult.getCachedKeys();
            try {
                cs.removeIf(cachedKeys::contains);
            } catch (UnsupportedOperationException e) {
                Class<?> declaringClass = invocation.getMethod().getDeclaringClass();
                throw SpExtExceptionEnum.METHOD_FIRST_ARG_UNMODIFIABLE_COLLECTION.newException("{}.{}", declaringClass.getName(), invocation.getMethod().getName());
            }
            if (log.isInfoEnabled()) {
                log.debug("ConfigureCache invoke method again, args:{} ", Jsons.str(cs));
            }
            Object res = super.invoke(invocation);
            if (Objects.isNull(res)) {
                return result;
            } else if (res instanceof PartialCacheResult) {
                // 上次缓存获取为null的keys，此时结果还是PartialCacheResult，表明其他地方缓存了数据，可能导致已有数据也不一致，报错。
                throw SpExtExceptionEnum.PARTIAL_CACHE_EXCEPTION.newException();
            } else if (result instanceof Map map) {
                map.putAll((Map<?, ?>) res);
            } else if (result instanceof Collection collection) {
                collection.addAll((Collection<?>) res);
            }
        }
        return result;
    }
}
