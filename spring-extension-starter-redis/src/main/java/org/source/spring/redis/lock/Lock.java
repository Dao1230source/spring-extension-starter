package org.source.spring.redis.lock;


import java.lang.annotation.*;

/**
 * 分布式锁
 * <pre>
 *     如果key指定的参数的值是集合（Collection）类型，自动批量处理
 * </pre>
 *
 * @author zengfugen
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Lock {
    /**
     * key 直接量，或spEl表达式
     *
     * @return lock key
     */
    String key();

    /**
     * 从方法的参数获取值的 spEl 表达式
     *
     * @return value for key
     */
    ValueParser valueParser() default @ValueParser();

    @interface ValueParser {
        /**
         * 从方法参数中解析值
         * <br>
         * 如果 expression 不为空，key中的参数占位符就不再和方法签名对应
         *
         * @return 解析后的值
         */
        String expression() default "";

        Class<?> type() default String.class;

    }
}
