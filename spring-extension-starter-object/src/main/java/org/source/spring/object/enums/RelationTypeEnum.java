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
    SUP_AND_SUB(RelationScopeEnum.HIERARCHY, 1, "上下级"),
    /**
     * 如文件夹
     */
    FOLDER(RelationScopeEnum.HIERARCHY, 2, "文件夹"),
    ;
    private final RelationScopeEnum scopeEnum;
    private final Integer type;
    private final String desc;


    @Override
    public Integer getScope() {
        return scopeEnum.getScope();
    }
}