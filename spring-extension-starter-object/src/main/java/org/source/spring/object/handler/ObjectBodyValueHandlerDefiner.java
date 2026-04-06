package org.source.spring.object.handler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true, value = {"objectId", "sorted", "relationType"})
public interface ObjectBodyValueHandlerDefiner {
    /**
     * name
     */
    String getName();

    void setName(String name);

    /**
     * 对象ID，唯一
     */
    String getObjectId();

    void setObjectId(String objectId);

    /**
     * 排序字段
     */
    String getSorted();

    void setSorted(String sorted);

    /**
     * 关联关系类型
     */
    Integer getRelationType();

    void setRelationType(Integer relationType);
}
