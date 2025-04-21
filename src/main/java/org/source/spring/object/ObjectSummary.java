package org.source.spring.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Data
public class ObjectSummary extends AbstractValue {
    /**
     * 父对象ID
     */
    private String parentObjectId;

    @JsonIgnore
    @Override
    public @NonNull String getId() {
        return getObjectId();
    }

    @JsonIgnore
    @Override
    public String getParentId() {
        return parentObjectId;
    }
}
