package org.source.spring.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.source.spring.object.enums.RelationTypeEnum;
import org.springframework.lang.Nullable;

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
