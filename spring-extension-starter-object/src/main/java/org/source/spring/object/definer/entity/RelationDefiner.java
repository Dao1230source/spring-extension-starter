package org.source.spring.object.definer.entity;

import org.source.spring.object.definer.enums.ObjectTypeDefiner;

import java.time.LocalDateTime;

public interface RelationDefiner {

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
     * 排序字段
     */
    String getSorted();

    void setSorted(String sorted);

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
