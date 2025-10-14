package org.source.spring.object;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.jetbrains.annotations.NotNull;
import org.source.utility.tree.define.Element;
import org.source.utility.tree.define.EnhanceElement;

import java.time.LocalDateTime;


@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ObjectElement<V extends AbstractValue> extends EnhanceElement<String> {

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
     * 排序
     */
    private String sorted;

    private Boolean deleted;

    private String createUser;

    private LocalDateTime createTime;

    private String updateUser;

    private LocalDateTime updateTime;

    /**
     * 对象ID，唯一
     */
    @EqualsAndHashCode.Exclude
    private String objectId;

    /**
     * 父对象ID
     */
    @EqualsAndHashCode.Exclude
    private String parentObjectId;

    @JsonIgnore
    @Override
    public @NonNull String getId() {
        return objectId;
    }

    @JsonIgnore
    @Override
    public String getParentId() {
        return parentObjectId;
    }

    @Override
    public int compareTo(@NotNull EnhanceElement<String> o) {
        return Element.comparator(this, (ObjectElement<V>) o, ObjectElement::getId);
    }
}