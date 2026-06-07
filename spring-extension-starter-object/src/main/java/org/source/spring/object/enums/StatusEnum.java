package org.source.spring.object.enums;

import org.jspecify.annotations.Nullable;

import java.util.Set;

public enum StatusEnum {
    /**
     * 已保存到数据
     */
    DATABASE,
    /**
     * 已缓存到Processor的Tree中
     */
    CACHED,
    /**
     * 已缓存到Processor的Tree中，且 object 部分改动
     */
    CACHED_OBJECT,
    /**
     * 已缓存到Processor的Tree中，且 object_body 部分改动
     */
    CACHED_OBJECT_BODY,
    /**
     * 已缓存到Processor的Tree中，且 relation 部分改动
     */
    CACHED_RELATION,
    /**
     * 已创建，未保存到Processor的Tree中
     */
    CREATED,
    ;

    public static final Set<StatusEnum> UPDATE_OBJECT_STATUSES = Set.of(CREATED, CACHED, CACHED_OBJECT);
    public static final Set<StatusEnum> UPDATE_OBJECT_BODY_STATUSES = Set.of(CREATED, CACHED, CACHED_OBJECT_BODY);
    public static final Set<StatusEnum> UPDATE_RELATION_STATUSES = Set.of(CREATED, CACHED, CACHED_RELATION);

    public static boolean updateObject(@Nullable StatusEnum status) {
        return UPDATE_OBJECT_STATUSES.contains(status);
    }

    public static boolean updateObjectBody(@Nullable StatusEnum status) {
        return UPDATE_OBJECT_BODY_STATUSES.contains(status);
    }

    public static boolean updateRelation(@Nullable StatusEnum status) {
        return UPDATE_RELATION_STATUSES.contains(status);
    }
}
