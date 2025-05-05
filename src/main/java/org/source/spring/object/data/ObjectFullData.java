package org.source.spring.object.data;


import lombok.*;
import org.source.spring.object.AbstractValue;
import org.source.utility.tree.identity.StringElement;

import java.time.LocalDateTime;


@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ObjectFullData<V extends AbstractValue> extends StringElement {

    /**
     * 对象ID，唯一
     */
    private String objectId;

    /**
     * 父对象ID
     */
    private String parentObjectId;


    /**
     * 关键字
     */
    private String key;

    /**
     * 值
     */
    private V value;

    /**
     * spaceId
     */
    private String spaceId;

    /**
     * 对象类型
     */
    private Integer type;

    /**
     * 关系类型
     */
    private Integer relationType;

    /**
     * 属主ID
     */
    private String belongId;

    private Boolean deleted;

    private String createUser;

    private LocalDateTime createTime;

    private String updateUser;

    private LocalDateTime updateTime;


    @Override
    public @NonNull String getId() {
        return objectId;
    }

    @Override
    public String getParentId() {
        return parentObjectId;
    }

}
