package org.source.spring.object.enums;

/**
 *
 */
public interface RelationTypeIdentity {

    /**
     * 领域
     */
    default Integer getScope() {
        return -1;
    }

    /**
     * 类型
     */
    default Integer getType() {
        return -1;
    }

}
