package org.source.spring.object.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RelationOperate extends RelationUniqueKey {

    private Integer type;
    private String sorted;
    private String relateOperate;
}
