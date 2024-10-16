package org.source.spring.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.source.utility.tree.identity.Element;

@Data
public abstract class ValueElementData implements ValueData, Element<String> {
    /**
     * 对象ID，唯一
     */
    @JsonIgnore
    private String objectId;
    /**
     * 对象状态
     */
    @JsonIgnore
    private ObjectStatusEnum objectStatus;

    @Override
    public int compareTo(@NotNull Element<String> o) {
        return this.getId().compareTo(o.getId());
    }
}
