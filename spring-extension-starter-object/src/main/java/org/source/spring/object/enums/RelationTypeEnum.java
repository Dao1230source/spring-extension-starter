package org.source.spring.object.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.source.spring.object.definer.enums.RelationTypeDefiner;

/**
 * 对象之间的关联关系类型
 */
@AllArgsConstructor
@Getter
public enum RelationTypeEnum implements RelationTypeDefiner {
    /**
     * 关联
     */
    LINKS(1, "关联"),
    /**
     * 组成部分
     */
    PARTS(2, "组成部分"),
    ;
    private final Integer type;
    private final String desc;
}