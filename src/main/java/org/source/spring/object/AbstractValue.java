package org.source.spring.object;

import lombok.Data;

@Data
public abstract class AbstractValue {
    /**
     * 对象ID，唯一
     */
    private String objectId;

    private String name;

    private String sorted;

    private Integer relationType;
}