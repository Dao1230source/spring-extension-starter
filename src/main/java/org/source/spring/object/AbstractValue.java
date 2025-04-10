package org.source.spring.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.source.utility.tree.identity.Element;

@Data
public abstract class AbstractValue implements Value, Element<String> {
    /**
     * 对象ID，唯一
     */
    @JsonIgnore
    private String objectId;

    @Override
    public int compareTo(@NotNull Element<String> o) {
        return this.getId().compareTo(o.getId());
    }
}
