package org.source.spring.object.data;

import lombok.Data;

@Data
public class RelationData {

    /**
     * 对象ID
     */
    private String objectId;
    /**
     * 父对象ID
     */
    private String parentObjectId;
    /**
     * 关联关系类型
     */
    private Integer type;

}
