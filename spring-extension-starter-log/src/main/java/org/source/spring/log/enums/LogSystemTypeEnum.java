package org.source.spring.log.enums;

import lombok.Getter;

@Getter
public enum LogSystemTypeEnum {
    /**
     * 默认值，不确定/不区分业务场景
     */
    DEFAULT(0, "默认"),
    /**
     * 前端通过浏览器访问
     */
    WEB(10, "网页访问"),
    /**
     * 后端通过接口调用
     */
    REST(20, "接口调用"),
    JOB(30, "定时任务"),
    MQ(40, "消息队列"),
    DATABASE(50, "数据库变更"),
    ;
    private final Integer type;
    private final String desc;

    LogSystemTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }
}
