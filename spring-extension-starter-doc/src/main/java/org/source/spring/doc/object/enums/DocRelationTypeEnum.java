package org.source.spring.doc.object.enums;

import lombok.Getter;
import org.source.spring.object.enums.RelationScopeEnum;
import org.source.spring.object.enums.RelationTypeDefiner;

/**
 * 对象之间的关联关系类型
 */
@Getter
public enum DocRelationTypeEnum implements RelationTypeDefiner {
    SUP_AND_SUB(RelationScopeEnum.SUP_AND_SUB, 10000, "上下级"),
    REQUEST(RelationScopeEnum.CONSTITUENT, 10001, "网络请求"),
    BASE_VARIABLE(RelationScopeEnum.LINK, 10002, "基础变量"),
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