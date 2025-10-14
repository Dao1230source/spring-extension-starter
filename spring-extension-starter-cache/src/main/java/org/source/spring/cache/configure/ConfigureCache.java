package org.source.spring.cache.configure;

import org.source.spring.cache.strategy.PartialCacheStrategyEnum;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.lang.annotation.*;

/**
 * 使用spring cache时，除{@link RedisCacheConfiguration}之外的扩展配置项
 *
 * @author zengfugen
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Cacheable
public @interface ConfigureCache {

    /**
     * 同{@link Cacheable} 的cacheNames
     *
     * @return cacheNames
     */
    @AliasFor(annotation = Cacheable.class)
    String[] cacheNames() default {};

    @AliasFor(annotation = Cacheable.class)
    String key() default "";

    /**
     * <pre>
     * 如果方法返回的值类型时{@literal  Collection}或{@literal Map}，并且想批量缓存，
     * </pre>
     *
     * @return cacheResolver bean name
     */
    @AliasFor(annotation = Cacheable.class)
    String cacheResolver() default "";

    @AliasFor(annotation = Cacheable.class)
    String condition() default "";

    /**
     * <pre>
     * Spring Expression Language (SpEL) expression
     * 当返回值是List/Set时必填
     * value生成redis key的spEl表达式
     * 使用 {@literal #P} 来表示spring EL 中的占位符
     * </pre>
     */
    String cacheKeySpEl() default "";

    /**
     * <pre>
     * {@literal ConfigureCacheConfig#assignValueClassesAndReturnType(Method, ConfigureCacheProperties)}
     * 会自动判断value是否是容器类型进行判断
     * 只有当缓存值确实是{@literal Collection/Map}时才需要指定{@link ReturnTypeEnum#RAW}
     * </pre>
     *
     * @return ReturnTypeEnum
     */
    ReturnTypeEnum returnType() default ReturnTypeEnum.AUTO;

    /**
     * @return PartialCacheStrategyEnum
     * @apiNote 警告：设置{@link  PartialCacheStrategyEnum#PARTIAL_TRUST}时，因为是两次请求的结果合并，所以有分布式事物风险
     */
    PartialCacheStrategyEnum partialCacheStrategy() default PartialCacheStrategyEnum.DISTRUST;

    CacheInRedis cacheInRedis() default @CacheInRedis;

    CacheInJvm cacheInJvm() default @CacheInJvm;

}
