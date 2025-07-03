package org.source.spring.object.entity;

import org.source.spring.object.enums.ObjectTypeDefiner;

import java.time.LocalDateTime;

public interface RelationEntityDefiner {

    Long getId();

    void setId(Long id);

    /**
     * 对象ID，唯一
     */
    String getObjectId();

    void setObjectId(String objectId);


    /**
     * 类型 {@link ObjectTypeDefiner#getType()}
     */
    Integer getType();

    void setType(Integer type);

    /**
     * 父objectId
     */
    String getParentObjectId();

    void setParentObjectId(String parentObjectId);

    /**
     * 属主ID，同属于一个对象ID
     */
    String getBelongId();

    void setBelongId(String parentObjectId);

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
