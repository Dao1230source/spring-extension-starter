package org.source.spring.object.definer.entity;

import org.source.spring.object.definer.enums.ObjectTypeDefiner;

/**
 * 对应数据库表实体类
 */
public interface ObjectEntityDefiner {

    Long getId();

    void setId(Long id);

    /**
     * 对象ID，唯一
     */
    String getObjectId();

    void setObjectId(String objectId);

    /**
     * 空间ID
     */
    String getSpaceId();

    void setSpaceId(String spaceId);

    /**
     * 类型 {@link ObjectTypeDefiner#getType()}
     */
    Integer getType();

    void setType(Integer type);


    /**
     * 是否已删除，0-未删除，1-已删除
     */
    Boolean getDeleted();

    void setDeleted(Boolean deleted);
}