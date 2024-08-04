package org.source.spring.log.enums;

import lombok.Getter;

@Getter
public enum PersistTypeEnum {
    SELECT("select", "查询"),
    INSERT("insert into", "新增"),
    UPDATE("update", "更新"),
    DELETE("delete from", "删除"),
    ;
    private final String keyword;
    private final String desc;

    PersistTypeEnum(String keyword, String desc) {
        this.keyword = keyword;
        this.desc = desc;
    }
}
