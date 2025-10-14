package org.source.spring.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public abstract class AbstractValue {
    /**
     * name
     */
    private String name;
    /**
     * 对象ID，唯一
     */
    @JsonIgnore
    private String objectId;
    /**
     * 排序字段
     */
    @JsonIgnore
    private String sorted;
    /**
     * 关联关系类型
     */
    @JsonIgnore
    private Integer relationType;
}