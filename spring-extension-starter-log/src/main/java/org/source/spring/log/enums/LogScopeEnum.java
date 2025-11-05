package org.source.spring.log.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LogScopeEnum {
    LOG("log", "普通日志"),
    DATA_SOURCE("data_source", "数据源"),
    LOG_CONTEXT("log_context", "日志上下文");
    private final String scope;
    private final String desc;
}