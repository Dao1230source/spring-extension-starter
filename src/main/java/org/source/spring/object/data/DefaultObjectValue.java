package org.source.spring.object.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.source.spring.object.ObjectValueElement;

@EqualsAndHashCode(callSuper = true)
@Data
public class DefaultObjectValue extends ObjectValueElement {
    private String parentObjectId;

    @Override
    public @NonNull String getId() {
        return this.getObjectId();
    }

    @Override
    public String getParentId() {
        return parentObjectId;
    }
}
