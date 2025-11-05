package org.source.spring.log.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.source.spring.uid.UidPrefix;

@Getter
@AllArgsConstructor
public enum LogPrefixEnum implements UidPrefix {
    LOG("L", "日志"),
    LOG_DATASOURCE("LD", "数据库变更日志"),
    ;
    private final String prefix;
    private final String desc;
}
