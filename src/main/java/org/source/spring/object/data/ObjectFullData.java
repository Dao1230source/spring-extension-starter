package org.source.spring.object.data;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.source.spring.object.AbstractValue;
import org.source.spring.object.StatusEnum;
import org.source.utility.tree.identity.StringElement;

import java.time.LocalDateTime;
import java.util.Objects;


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
     * 名称
     */
    private String name;

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

    /**
     * node status
     */
    private StatusEnum status;

    @JsonIgnore
    @Override
    public @NonNull String getId() {
        if (Objects.isNull(objectId)) {
            return value.getId();
        }
        return objectId;
    }

    @JsonIgnore
    @Override
    public String getParentId() {
        if (Objects.isNull(parentObjectId)) {
            return value.getParentId();
        }
        return parentObjectId;
    }

}