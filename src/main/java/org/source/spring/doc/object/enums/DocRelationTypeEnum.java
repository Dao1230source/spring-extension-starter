package org.source.spring.doc.object.enums;

import lombok.Getter;
import org.source.spring.object.enums.RelationScopeEnum;
import org.source.spring.object.enums.RelationTypeIdentity;

/**
* 对象之间的关联关系类型
*/
@Getter
public enum DocRelationTypeEnum implements RelationTypeIdentity {
    BELONG(RelationScopeEnum.BELONG, 10000, "上下级"),
    VAR_ANNOTATION(RelationScopeEnum.CONSTITUENT, 10001, "变量注解"),
    ;
    private final RelationScopeEnum relationScope;
    private final Integer type;
    private final String desc;

    DocRelationTypeEnum(RelationScopeEnum relationScope, Integer type, String desc) {
        this.relationScope = relationScope;
        this.type = type;
        this.desc = desc;
    }


    @Override
    public Integer getScope() {
        return relationScope.getScope();
    }
}