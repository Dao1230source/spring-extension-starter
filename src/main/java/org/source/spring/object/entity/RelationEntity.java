package org.source.spring.object.entity;

import lombok.Data;
import org.source.spring.object.processor.RelationHandler;

import java.time.LocalDateTime;

@Data
public class RelationEntity {

    private Long id;

    /**
     * 对象ID
     */
    private String objectId;
    /**
     * 父对象ID
     */
    private String parentObjectId;
    /**
     * 关联关系类型 {@link RelationHandler#getType()}
     */
    private Integer type;
    /**
     * 创建人
     */
    private String createUser;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
