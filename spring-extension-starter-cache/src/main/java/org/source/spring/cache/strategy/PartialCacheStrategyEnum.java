package org.source.spring.cache.strategy;

public enum PartialCacheStrategyEnum {
    /**
     * 信任缓存
     */
    TRUST,
    /**
     * 不信任缓存
     */
    DISTRUST,
    /**
     * 信任缓存，但缓存返回为null部分重新通过执行方法获取
     * <pre>
     * 使用该策略需要遵循以下条件：
     *  1、方法有且只有一个参数
     *  2、入参是Collection类型，且是可变的，不能通过以下方法创建{@code
     *  List.of()
     *  List.copyOf()
     *  Set.of()
     *  Set.copyOf()
     *  Collections.unmodifiableCollection(cs)
     *  }
     *  3、{@literal ConfigureCache.key()}无值或指定方法第一个参数
     * </pre>
     */
    PARTIAL_TRUST
}
