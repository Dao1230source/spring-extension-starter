package org.source.spring.object.entity;

import org.source.spring.object.processor.ObjectIdentity;

import java.time.LocalDateTime;

public interface ObjectEntityIdentity {

    Long getId();

    void setId(Long id);

    /**
     * 对象ID，唯一
     */
    String getObjectId();

    void setObjectId(String objectId);

    /**
     * 类型 {@link ObjectIdentity#getType()}
     */
    Integer getType();

    void setType(Integer type);

    /**
     * 空间ID
     */
    String getSpaceId();

    void setSpaceId(String spaceId);

    /**
     * 是否已删除，0-未删除，1-已删除
     */
    Boolean getDeleted();

    void setDeleted(Boolean deleted);

    /**
     * 创建人
     */
    String getCreateUser();

    void setCreateUser(String createUser);

    /**
     * 创建时间
     */
    LocalDateTime getCreateTime();

    void setCreateTime(LocalDateTime createTime);
}