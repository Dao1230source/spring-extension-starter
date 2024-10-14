package org.source.spring.object.entity;

import lombok.Data;
import org.source.spring.object.processor.ObjectHandler;

import java.time.LocalDateTime;

@Data
public class ObjectEntity {

    private Long id;

    /**
     * 对象ID，唯一
     */
    private String objectId;

    /**
     * 关键字
     */
    private String key;
    /**
     * 值,json
     */
    private String value;
    /**
     * 类型 {@link ObjectHandler#getType()}
     */
    private Integer type;
    /**
     * 是否删除，0-未删除，1-已删除
     */
    private Boolean deleted;
    /**
     * 创建人
     */
    private String createUser;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新人
     */
    private String updateUser;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
