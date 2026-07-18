package org.source.spring.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jspecify.annotations.Nullable;

import java.util.Map;

@Data
public class ObjectBodyData {

    private String name;
    private String sorted;

    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private String objectId;
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private @Nullable String parentObjectId;
    /**
     * 与父级数据的关联关系
     */
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private @Nullable Map<String, Integer> relationTypeMap;
}
