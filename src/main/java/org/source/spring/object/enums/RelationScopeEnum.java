package org.source.spring.object.enums;

import lombok.Getter;

/**
 * 对象之间的关联关系类型
 */
@Getter
public enum RelationScopeEnum {
    /**
     * 互相之间组成一个整体，比如多个对象组成的视图
     */
    CONSTITUENT(0, "组成部分"),
    /**
     * 如文件夹
     */
    BELONG(1, "从属上下级"),
    /**
     * 如流程图之间的连接，平级
     */
    LINK(2, "连接关联"),
    /**
     * 不是人为直接关联的，随时可变化的关联，如系统推荐
     */
    REFERENCE(3, "参考引用"),
    ;
    private final Integer scope;
    private final String desc;

    RelationScopeEnum(Integer scope, String desc) {
        this.scope = scope;
        this.desc = desc;
    }
}