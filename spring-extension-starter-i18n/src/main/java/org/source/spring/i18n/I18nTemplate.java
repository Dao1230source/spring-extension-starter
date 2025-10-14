package org.source.spring.i18n;

import lombok.extern.slf4j.Slf4j;
import org.source.spring.cache.configure.CacheInJvm;
import org.source.spring.cache.configure.CacheInRedis;
import org.source.spring.cache.configure.ConfigureCache;
import org.source.spring.cache.configure.ReturnTypeEnum;
import org.source.spring.cache.strategy.PartialCacheStrategyEnum;
import org.source.spring.i18n.facade.data.Dict;
import org.source.spring.i18n.facade.param.Dict2Param;
import org.source.spring.i18n.facade.param.Dict3Param;
import org.source.spring.i18n.facade.param.Dict4Param;
import org.source.spring.i18n.processor.Processor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
public class I18nTemplate<E extends Dict> {

    public static final String LOCALE_GROUP_KEY = "LOCALE_GROUP_KEY";
    public static final String LOCALE_GROUP = "LOCALE_GROUP";
    public static final String ALL_LOCALES = "ALL_LOCALES";

    private final Processor<E> processor;

    public I18nTemplate(Processor<E> processor) {
        this.processor = processor;
    }

    /**
     * 因为需要缓存，将 Optional 拆箱
     *
     * @param param Dict3Param
     * @return E
     */
    @ConfigureCache(cacheNames = LOCALE_GROUP_KEY, key = "T(org.source.spring.i18n.facade.param.Dict3Param).uniqueKey3(#param)",
            cacheInRedis = @CacheInRedis(enable = false),
            cacheInJvm = @CacheInJvm(enable = true))
    public @Nullable E findByKey(Dict3Param param) {
        return processor.findByKey(param).orElse(null);
    }

    @ConfigureCache(cacheNames = LOCALE_GROUP, returnType = ReturnTypeEnum.RAW,
            partialCacheStrategy = PartialCacheStrategyEnum.PARTIAL_TRUST,
            key = "T(org.source.spring.i18n.facade.param.Dict3Param).uniqueKeys3(#params)",
            cacheInRedis = @CacheInRedis(enable = false),
            cacheInJvm = @CacheInJvm(enable = true))
    public List<E> findByKeys(Collection<Dict3Param> params) {
        if (CollectionUtils.isEmpty(params)) {
            return List.of();
        }
        return processor.findByKeys(params);
    }

    @ConfigureCache(cacheNames = LOCALE_GROUP, returnType = ReturnTypeEnum.RAW,
            key = "T(org.source.spring.i18n.facade.param.Dict2Param).uniqueKey2(#param)",
            cacheInRedis = @CacheInRedis(enable = false),
            cacheInJvm = @CacheInJvm(enable = true))
    public List<E> findByGroup(Dict2Param param) {
        return processor.findByGroup(param);
    }

    @ConfigureCache(cacheNames = LOCALE_GROUP, returnType = ReturnTypeEnum.RAW,
            partialCacheStrategy = PartialCacheStrategyEnum.PARTIAL_TRUST,
            key = "T(org.source.spring.i18n.facade.param.Dict2Param).uniqueKeys2(#params)",
            cacheInRedis = @CacheInRedis(enable = false),
            cacheInJvm = @CacheInJvm(enable = true))
    public Map<Dict2Param, List<E>> findByGroups(Collection<Dict2Param> params) {
        if (CollectionUtils.isEmpty(params)) {
            return Map.of();
        }
        return processor.findByGroups(params);
    }

    @ConfigureCache(cacheNames = ALL_LOCALES, key = "#root.methodName", returnType = ReturnTypeEnum.RAW,
            cacheInRedis = @CacheInRedis(enable = false),
            cacheInJvm = @CacheInJvm(enable = true))
    public List<String> findAllLocales() {
        return processor.findAllScopes();
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = LOCALE_GROUP_KEY, key = "T(org.source.spring.i18n.facade.param.Dict3Param).uniqueKey3(#param)"),
            @CacheEvict(cacheNames = LOCALE_GROUP, key = "T(org.source.spring.i18n.facade.param.Dict2Param).uniqueKey2(#param)")
    })
    public int save(Dict4Param param) {
        return processor.save(param);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = LOCALE_GROUP_KEY, key = "T(org.source.spring.i18n.facade.param.Dict3Param).uniqueKeys3(#params)"),
            @CacheEvict(cacheNames = LOCALE_GROUP, key = "T(org.source.spring.i18n.facade.param.Dict2Param).uniqueKeys2(#params)")
    })
    public int saveBatch(Collection<Dict4Param> params) {
        if (CollectionUtils.isEmpty(params)) {
            return 0;
        }
        return processor.saveBatch(params);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = LOCALE_GROUP_KEY, key = "T(org.source.spring.i18n.facade.param.Dict3Param).uniqueKey3(#param)"),
            @CacheEvict(cacheNames = LOCALE_GROUP, key = "T(org.source.spring.i18n.facade.param.Dict2Param).uniqueKey2(#param)")
    })
    public int removeByKey(Dict3Param param) {
        return processor.removeByKey(param);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = LOCALE_GROUP_KEY, key = "T(org.source.spring.i18n.facade.param.Dict3Param).uniqueKeys3(#params)"),
            @CacheEvict(cacheNames = LOCALE_GROUP, key = "T(org.source.spring.i18n.facade.param.Dict2Param).uniqueKeys2(#params)")
    })
    public int removeByKeys(Collection<Dict3Param> params) {
        if (CollectionUtils.isEmpty(params)) {
            return 0;
        }
        return processor.removeByKeys(params);
    }


    @Caching(evict = {
            @CacheEvict(cacheNames = LOCALE_GROUP_KEY, key = "T(org.source.spring.i18n.facade.param.Dict2Param).uniqueKeys3WhenRemove(#param)"),
            @CacheEvict(cacheNames = LOCALE_GROUP, key = "T(org.source.spring.i18n.facade.param.Dict2Param).uniqueKey2(#param)")
    })
    public int removeByGroup(Dict2Param param) {
        return processor.removeByGroup(param);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = LOCALE_GROUP_KEY, key = "T(org.source.spring.i18n.facade.param.Dict2Param).uniqueKeys3WhenRemove(#params)"),
            @CacheEvict(cacheNames = LOCALE_GROUP, key = "T(org.source.spring.i18n.facade.param.Dict2Param).uniqueKeys2(#params)")
    })
    public int removeByGroups(Collection<Dict2Param> params) {
        if (CollectionUtils.isEmpty(params)) {
            return 0;
        }
        return processor.removeByGroups(params);
    }
}
