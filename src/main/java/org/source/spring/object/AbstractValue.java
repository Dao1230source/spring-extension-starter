package org.source.spring.object;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.source.utility.tree.identity.Element;

@Data
public abstract class AbstractValue implements Element<String> {
    /**
     * 对象ID，唯一
     */
    private String objectId;

    @Override
    public int compareTo(@NotNull Element<String> o) {
        return this.getId().compareTo(o.getId());
    }
}
