package org.source.spring.object;

import java.util.Set;

public enum StatusEnum {
    DATABASE,
    CACHED,
    CACHED_OBJECT,
    CACHED_OBJECT_BODY,
    CACHED_RELATION,
    CREATED,
    ;

    public static final Set<StatusEnum> UPDATE_OBJECT_STATUSES = Set.of(CREATED, CACHED, CACHED_OBJECT);
    public static final Set<StatusEnum> UPDATE_OBJECT_BODY_STATUSES = Set.of(CREATED, CACHED, CACHED_OBJECT_BODY);
    public static final Set<StatusEnum> UPDATE_RELATION_STATUSES = Set.of(CREATED, CACHED, CACHED_RELATION);

    public static boolean updateObject(StatusEnum status) {
        return UPDATE_OBJECT_STATUSES.contains(status);
    }

    public static boolean updateObjectBody(StatusEnum status) {
        return UPDATE_OBJECT_BODY_STATUSES.contains(status);
    }

    public static boolean updateRelation(StatusEnum status) {
        return UPDATE_RELATION_STATUSES.contains(status);
    }
}
