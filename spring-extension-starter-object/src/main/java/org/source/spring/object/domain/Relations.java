package org.source.spring.object.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.source.spring.object.enums.RelateTypeEnum;

@EqualsAndHashCode(callSuper = true)
@Data
public class Relations extends RelationKey {

    private Integer type;
    private String sort;
    /**
     * 关联操作，新增还是删除 {@link RelateTypeEnum}
     */
    private Integer relateType;

}
