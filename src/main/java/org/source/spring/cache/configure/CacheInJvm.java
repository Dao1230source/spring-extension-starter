package org.source.spring.cache.configure;

import org.source.spring.cache.constant.CacheConstant;

import java.lang.annotation.*;

@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheInJvm {
    boolean enable() default false;

    long ttl() default CacheConstant.FROM_CONFIG;

    long jvmMaxSize() default CacheConstant.NO_LIMIT;

    /**
     * <pre>
     *  单条数据时，key={@link ConfigureCache#key()}指定的值
     *  多条数据时，
     *      返回值是{@literal Map<K,V>}，key=K
     *      返回值是集合时，key=经过{@link ConfigureCache#cacheKeySpEl()}计算之后的{@literal List<E>/Set<E>}的E
     * </pre>
     *
     * @return class
     */
    Class<?> keyClass() default String.class;
}
