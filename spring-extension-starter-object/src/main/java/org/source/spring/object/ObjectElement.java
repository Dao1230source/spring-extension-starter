package org.source.spring.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.jspecify.annotations.Nullable;
import org.source.utility.tree.define.EnhanceElement;

@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ObjectElement<D extends BodyData> extends EnhanceElement<String> {

    /**
     * 值
     */
    private D data;

    /**
     * spaceId
     */
    private String spaceId;

    /**
     * 对象类型
     */
    private Integer type;

    @JsonIgnore
    @Override
    public String getId() {
        return data.getObjectId();
    }

    @JsonIgnore
    @Override
    public @Nullable String getParentId() {
        return data.getParentObjectId();
    }

    @Override
    public int compareTo(@Nullable EnhanceElement<String> o) {
        return EnhanceElement.comparator(this, (ObjectElement<D>) o, ObjectElement::getId);
    }
}