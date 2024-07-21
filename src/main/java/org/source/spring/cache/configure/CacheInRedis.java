package org.source.spring.cache.configure;

import org.source.spring.cache.constant.CacheConstant;

import java.lang.annotation.*;

@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheInRedis {
    boolean enable() default true;

    long ttl() default CacheConstant.FROM_CONFIG;

    /**
     * <pre>
     * redis上缓存的单条数据的java类型
     * 当key单条时，value类型=方法返回值类型
     * 当key批量时，value类型=方法返回值容器中对象{@literal （Map<K,V>的V,List<E>,Set<E>的E）}的类型
     * </pre>
     *
     * @return classes
     */
    Class<?>[] valueClasses() default {};
}
