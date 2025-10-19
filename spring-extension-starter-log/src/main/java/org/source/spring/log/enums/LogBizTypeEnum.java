package org.source.spring.log.enums;

import lombok.Getter;

@Getter
public enum LogBizTypeEnum {
    /**
     * 默认值，不确定/不区分业务场景
     */
    DEFAULT(0, "默认"),
    /**
     * 用户相关
     */
    USER(1, "用户"),
    ;
    private final Integer type;
    private final String desc;

    LogBizTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }
}
