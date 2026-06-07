package org.source.spring.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jspecify.annotations.Nullable;
import org.source.spring.object.enums.RelationTypeEnum;

@Data
public class ObjectBodyData {
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private String objectId;
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private @Nullable String parentObjectId;
    private String sorted;
    private Integer relationType = RelationTypeEnum.SUP_AND_SUB.getType();
}
