package org.source.spring.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.source.utility.tree.identity.StringElement;

@Builder
@EqualsAndHashCode(callSuper = false)
@Data
public class ViewItemData extends StringElement {
    /**
     * 对象ID，唯一
     */
    private String objectId;
    /**
     * 父对象ID
     */
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
}
