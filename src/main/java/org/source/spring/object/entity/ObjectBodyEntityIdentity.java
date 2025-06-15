package org.source.spring.object.entity;

import java.time.LocalDateTime;

public interface ObjectBodyEntityIdentity {


    Long getId();

    void setId(Long id);


    /**
     * 对象ID，唯一
     */

    String getObjectId();

    void setObjectId(String objectId);


    /**
     * name
     */

    String getName();

    void setName(String name);


    /**
     * value
     * <p>
     * json 格式
     */
    String getValue();

    void setValue(String value);

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


    /**
     * 更新人
     */
    String getUpdateUser();

    void setUpdateUser(String updateUser);

    /**
     * 更新时间
     */
    LocalDateTime getUpdateTime();

    void setUpdateTime(LocalDateTime updateTime);

}
