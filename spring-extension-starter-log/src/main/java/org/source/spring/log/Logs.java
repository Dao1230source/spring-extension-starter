package org.source.spring.log;

import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.source.spring.log.enums.LogBizTypeEnum;
import org.source.spring.log.enums.LogSystemTypeEnum;
import org.source.spring.trace.TraceContext;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@UtilityClass
@Slf4j
public class Logs {
    private static LogDataProcessor logDataProcessor;

    public static synchronized void setLogDataProcessor(LogDataProcessor logDataProcessor) {
        Logs.logDataProcessor = logDataProcessor;
    }

    public static void save(List<LogData> logDataList) {
        if (Objects.nonNull(logDataProcessor)) {
            logDataProcessor.save(logDataList);
        }
    }

    public static void save(LogData logData) {
        save(List.of(logData));
    }

    public static void put(String scopeName) {
        LogContext.init(scopeName);
    }

    public static void put(String scopeName, String k, Object v) {
        LogContext.set(scopeName, k, v);
    }

    public static @Nullable Object get(String scopeName, String k) {
        return LogContext.get(scopeName, k);
    }

    public static void remove(String scopeName) {
        LogContext.clear(scopeName);
    }

    public static void clear() {
        LogContext.clear();
    }

    public static void putLog() {
        LogContext.init(LogConstants.VARIABLES_LOG);
    }

    public static void removeLog() {
        LogContext.clear(LogConstants.VARIABLES_LOG);
    }

    public static @Nullable String getLogId() {
        return LogContext.get(LogConstants.VARIABLES_LOG, LogConstants.LOG_ID);
    }

    public static void setLogId(String logId) {
        LogContext.set(LogConstants.VARIABLES_LOG, LogConstants.LOG_ID, logId);
    }

    public static @Nullable String getRefId() {
        return LogContext.get(LogConstants.VARIABLES_LOG, LogConstants.REF_ID);
    }

    public static void setRefId(String refId) {
        LogContext.set(LogConstants.VARIABLES_LOG, LogConstants.REF_ID, refId);
    }

    public static @Nullable String getDesc() {
        return LogContext.get(LogConstants.VARIABLES_LOG, LogConstants.DESC);
    }

    public static void setDesc(String desc) {
        LogContext.set(LogConstants.VARIABLES_LOG, LogConstants.DESC, desc);
    }

    public static Integer getSystemType() {
        Integer systemType = LogContext.get(LogConstants.VARIABLES_LOG, LogConstants.SYSTEM_TYPE);
        if (Objects.isNull(systemType) || LogSystemTypeEnum.DEFAULT.getType().equals(systemType)) {
            systemType = LogContext.find(LogConstants.VARIABLES_LOG_CONTEXT, LogConstants.SYSTEM_TYPE);
        }
        if (Objects.isNull(systemType)) {
            systemType = LogSystemTypeEnum.DEFAULT.getType();
        }
        return systemType;
    }

    public static Integer getSystemType(LogSystemTypeEnum systemType) {
        // 非默认值，直接返回
        if (!LogSystemTypeEnum.DEFAULT.equals(systemType)) {
            return systemType.getType();
        }
        return getSystemType();
    }

    public static void setSystemType(LogSystemTypeEnum systemType) {
        if (!LogSystemTypeEnum.DEFAULT.equals(systemType)) {
            LogContext.set(LogConstants.VARIABLES_LOG, LogConstants.SYSTEM_TYPE, systemType.getType());
        }
    }

    public static @Nullable String getParentLogId() {
        return LogContext.get(LogConstants.VARIABLES_LOG, LogConstants.PARENT_LOG_ID);
    }

    public static void setParentLogId(String parentLogId) {
        LogContext.set(LogConstants.VARIABLES_LOG, LogConstants.PARENT_LOG_ID, parentLogId);
    }

    public static @Nullable String getTitle() {
        return LogContext.get(LogConstants.VARIABLES_LOG, LogConstants.TITLE);
    }

    public static void setTitle(String title) {
        LogContext.set(LogConstants.VARIABLES_LOG, LogConstants.TITLE, title);
    }

    public static @Nullable String getUserId() {
        return LogContext.get(LogConstants.VARIABLES_LOG, LogConstants.USER_ID);
    }

    public static void setUserId(String userId) {
        LogContext.set(LogConstants.VARIABLES_LOG, LogConstants.USER_ID, userId);
    }

    public static Integer getBizType() {
        Integer bizType = LogContext.get(LogConstants.VARIABLES_LOG, LogConstants.BIZ_TYPE);
        if (Objects.isNull(bizType) || LogBizTypeEnum.DEFAULT.getType().equals(bizType)) {
            bizType = LogContext.find(LogConstants.VARIABLES_LOG_CONTEXT, LogConstants.BIZ_TYPE);
        }
        if (Objects.isNull(bizType)) {
            bizType = LogBizTypeEnum.DEFAULT.getType();
        }
        return bizType;
    }

    public static Integer getBizType(LogBizTypeEnum bizType) {
        if (!LogBizTypeEnum.DEFAULT.equals(bizType)) {
            return bizType.getType();
        }
        return getBizType();
    }

    public static void setBizType(LogBizTypeEnum bizType) {
        if (!LogBizTypeEnum.DEFAULT.equals(bizType)) {
            LogContext.set(LogConstants.VARIABLES_LOG, LogConstants.BIZ_TYPE, bizType.getType());
        }
    }

    public static @Nullable Object getParam() {
        return LogContext.get(LogConstants.VARIABLES_LOG, LogConstants.PARAM);
    }

    public static void setParam(Object param) {
        LogContext.set(LogConstants.VARIABLES_LOG, LogConstants.PARAM, param);
    }

    public static @Nullable Object getResult() {
        return LogContext.get(LogConstants.VARIABLES_LOG, LogConstants.RESULT);
    }

    public static void setResult(Object result) {
        LogContext.set(LogConstants.VARIABLES_LOG, LogConstants.RESULT, result);
    }

    public static @Nullable Object getExtra() {
        return LogContext.get(LogConstants.VARIABLES_LOG, LogConstants.EXTRA);
    }

    public static @Nullable String getMethodLocation() {
        return LogContext.get(LogConstants.VARIABLES_LOG, LogConstants.METHOD_LOCATION);
    }

    public static void setExtra(Object extra) {
        LogContext.set(LogConstants.VARIABLES_LOG, LogConstants.EXTRA, extra);
    }

    public static void putLogContext() {
        LogContext.init(LogConstants.VARIABLES_LOG_CONTEXT);
    }

    public static void removeLogContext() {
        LogContext.clear(LogConstants.VARIABLES_LOG_CONTEXT);
    }

    public static void setLogContextSystemType(LogSystemTypeEnum systemType) {
        if (!LogSystemTypeEnum.DEFAULT.equals(systemType)) {
            LogContext.set(LogConstants.VARIABLES_LOG_CONTEXT, LogConstants.SYSTEM_TYPE, systemType.getType());
        }
    }

    public static void setLogContextBizType(LogBizTypeEnum bizType) {
        if (!LogBizTypeEnum.DEFAULT.equals(bizType)) {
            LogContext.set(LogConstants.VARIABLES_LOG_CONTEXT, LogConstants.BIZ_TYPE, bizType.getType());
        }
    }

    public static void putDataSource() {
        LogContext.init(LogConstants.VARIABLES_DATA_SOURCE);
    }

    public static void removeDataSource() {
        LogContext.clear(LogConstants.VARIABLES_DATA_SOURCE);
    }

    public static boolean getDataSourceEnabled() {
        Object o = LogContext.get(LogConstants.VARIABLES_DATA_SOURCE, LogConstants.ENABLED);
        if (Objects.nonNull(o)) {
            return Boolean.parseBoolean(o.toString());
        }
        return false;
    }

    public static String getDataSourceParentLogId() {
        String parentLogId = LogContext.get(LogConstants.VARIABLES_DATA_SOURCE, LogConstants.PARENT_LOG_ID);
        if (!StringUtils.hasText(parentLogId)) {
            parentLogId = TraceContext.getTraceId();
        }
        return parentLogId;
    }

    public static String getDataSourceRefId() {
        String refId = LogContext.get(LogConstants.VARIABLES_DATA_SOURCE, LogConstants.REF_ID);
        if (!StringUtils.hasText(refId)) {
            refId = TraceContext.getTraceId();
        }
        return refId;
    }

    public static @Nullable List<String> getDataSourceKeyColumns() {
        return LogContext.get(LogConstants.VARIABLES_DATA_SOURCE, LogConstants.KEY_COLUMNS);
    }

    public static @Nullable List<String> getDataSourceExcludeTableNames() {
        return LogContext.get(LogConstants.VARIABLES_DATA_SOURCE, LogConstants.EXCLUDE_TABLE_NAMES);
    }

    public static @Nullable List<String> getDataSourceExcludeColumns() {
        return LogContext.get(LogConstants.VARIABLES_DATA_SOURCE, LogConstants.EXCLUDE_COLUMNS);
    }

    public static void setDataSourceEnabled(boolean enabled) {
        LogContext.set(LogConstants.VARIABLES_DATA_SOURCE, LogConstants.ENABLED, enabled);
    }

    public static void setDataSourceKeyColumns(List<String> keyColumns) {
        LogContext.set(LogConstants.VARIABLES_DATA_SOURCE, LogConstants.KEY_COLUMNS, keyColumns);
    }

    public static void setDataSourceExcludeTableNames(List<String> excludeTableNames) {
        LogContext.set(LogConstants.VARIABLES_DATA_SOURCE, LogConstants.EXCLUDE_TABLE_NAMES, excludeTableNames);
    }

    public static void setDataSourceExcludeColumns(List<String> excludeColumns) {
        LogContext.set(LogConstants.VARIABLES_DATA_SOURCE, LogConstants.EXCLUDE_COLUMNS, excludeColumns);
    }

    public static void setDataSourceParentLogId(String parentLogId) {
        LogContext.set(LogConstants.VARIABLES_DATA_SOURCE, LogConstants.PARENT_LOG_ID, parentLogId);
    }

    public static void setDataSourceRefId(String refId) {
        LogContext.set(LogConstants.VARIABLES_DATA_SOURCE, LogConstants.REF_ID, refId);
    }
}