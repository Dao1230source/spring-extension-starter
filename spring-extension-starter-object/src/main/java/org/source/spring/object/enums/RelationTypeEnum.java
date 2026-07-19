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
     * 上下级
     */
    SUP_AND_SUB(1, "上下级"),
    /**
     * 如文件夹
     */
    FOLDER(2, "文件夹"),
    ;
    private final Integer type;
    private final String desc;
}