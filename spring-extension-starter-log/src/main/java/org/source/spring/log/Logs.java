package org.source.spring.log;

import jakarta.annotation.Nullable;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.source.spring.log.enums.LogBizTypeEnum;
import org.source.spring.log.enums.LogScopeEnum;
import org.source.spring.log.enums.LogSystemTypeEnum;

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

    public static void init(LogScopeEnum scope) {
        LogContext.init(scope.getScope());
    }

    public static void remove(LogScopeEnum scope) {
        LogContext.clear(scope.getScope());
    }

    public static void put(String scopeName, String k, Object v) {
        LogContext.set(scopeName, k, v);
    }

    public static @Nullable Object get(String scopeName, String k) {
        return LogContext.get(scopeName, k);
    }

    public static void clear() {
        LogContext.clear();
    }

    public static void putLog(LogScopeEnum scope) {
        LogContext.init(scope.getScope());
    }

    public static void removeLog(LogScopeEnum scope) {
        LogContext.clear(scope.getScope());
    }

    public static @Nullable String getLogId(LogScopeEnum scope) {
        return LogContext.get(scope.getScope(), LogConstants.LOG_ID);
    }

    public static void setLogId(LogScopeEnum scope, String logId) {
        LogContext.set(scope.getScope(), LogConstants.LOG_ID, logId);
    }

    public static @Nullable String getParentLogId(LogScopeEnum scope) {
        return LogContext.get(scope.getScope(), LogConstants.PARENT_LOG_ID);
    }

    public static void setParentLogId(LogScopeEnum scope, String parentLogId) {
        LogContext.set(scope.getScope(), LogConstants.PARENT_LOG_ID, parentLogId);
    }


    public static @Nullable String getRefId(LogScopeEnum scope) {
        return LogContext.get(scope.getScope(), LogConstants.REF_ID);
    }

    public static void setRefId(LogScopeEnum scope, String refId) {
        LogContext.set(scope.getScope(), LogConstants.REF_ID, refId);
    }

    public static @Nullable String getTitle(LogScopeEnum scope) {
        return LogContext.get(scope.getScope(), LogConstants.TITLE);
    }

    public static void setTitle(LogScopeEnum scope, String title) {
        LogContext.set(scope.getScope(), LogConstants.TITLE, title);
    }

    public static @Nullable String getDesc(LogScopeEnum scope) {
        return LogContext.get(scope.getScope(), LogConstants.DESC);
    }

    public static void setDesc(LogScopeEnum scope, String desc) {
        LogContext.set(scope.getScope(), LogConstants.DESC, desc);
    }

    public static Integer getSystemType(LogScopeEnum scope) {
        Integer systemType = LogContext.get(scope.getScope(), LogConstants.SYSTEM_TYPE);
        if (Objects.isNull(systemType) || LogSystemTypeEnum.DEFAULT.getType().equals(systemType)) {
            systemType = LogContext.find(LogScopeEnum.LOG_CONTEXT.getScope(), LogConstants.SYSTEM_TYPE);
        }
        if (Objects.isNull(systemType)) {
            systemType = LogSystemTypeEnum.DEFAULT.getType();
        }
        return systemType;
    }

    public static Integer getSystemType(LogScopeEnum scope, LogSystemTypeEnum systemType) {
        // 非默认值，直接返回
        if (!LogSystemTypeEnum.DEFAULT.equals(systemType)) {
            return systemType.getType();
        }
        return getSystemType(scope);
    }

    public static void setSystemType(LogScopeEnum scope, LogSystemTypeEnum systemType) {
        if (!LogSystemTypeEnum.DEFAULT.equals(systemType)) {
            LogContext.set(scope.getScope(), LogConstants.SYSTEM_TYPE, systemType.getType());
        }
    }

    public static Integer getBizType(LogScopeEnum scope) {
        Integer bizType = LogContext.get(scope.getScope(), LogConstants.BIZ_TYPE);
        if (Objects.isNull(bizType) || LogBizTypeEnum.DEFAULT.getType().equals(bizType)) {
            bizType = LogContext.find(LogScopeEnum.LOG_CONTEXT.getScope(), LogConstants.BIZ_TYPE);
        }
        if (Objects.isNull(bizType)) {
            bizType = LogBizTypeEnum.DEFAULT.getType();
        }
        return bizType;
    }

    public static Integer getBizType(LogScopeEnum scope, LogBizTypeEnum bizType) {
        if (!LogBizTypeEnum.DEFAULT.equals(bizType)) {
            return bizType.getType();
        }
        return getBizType(scope);
    }

    public static void setBizType(LogScopeEnum scope, LogBizTypeEnum bizType) {
        if (!LogBizTypeEnum.DEFAULT.equals(bizType)) {
            LogContext.set(scope.getScope(), LogConstants.BIZ_TYPE, bizType.getType());
        }
    }

    public static @Nullable String getUserId(LogScopeEnum scope) {
        return LogContext.get(scope.getScope(), LogConstants.USER_ID);
    }

    public static void setUserId(LogScopeEnum scope, String userId) {
        LogContext.set(scope.getScope(), LogConstants.USER_ID, userId);
    }

    public static @Nullable Object getParam(LogScopeEnum scope) {
        return LogContext.get(scope.getScope(), LogConstants.PARAM);
    }

    public static void setParam(LogScopeEnum scope, Object param) {
        LogContext.set(scope.getScope(), LogConstants.PARAM, param);
    }

    public static @Nullable Object getResult(LogScopeEnum scope) {
        return LogContext.get(scope.getScope(), LogConstants.RESULT);
    }

    public static void setResult(LogScopeEnum scope, Object result) {
        LogContext.set(scope.getScope(), LogConstants.RESULT, result);
    }

    public static @Nullable Object getExtra(LogScopeEnum scope) {
        return LogContext.get(scope.getScope(), LogConstants.EXTRA);
    }

    public static void setExtra(LogScopeEnum scope, Object extra) {
        LogContext.set(scope.getScope(), LogConstants.EXTRA, extra);
    }

    public static @Nullable String getMethodLocation(LogScopeEnum scope) {
        return LogContext.get(scope.getScope(), LogConstants.METHOD_LOCATION);
    }

    /*
    LogContext
     */
    public static void setLogContextSystemType(LogSystemTypeEnum systemType) {
        if (!LogSystemTypeEnum.DEFAULT.equals(systemType)) {
            LogContext.set(LogScopeEnum.LOG_CONTEXT.getScope(), LogConstants.SYSTEM_TYPE, systemType.getType());
        }
    }

    public static void setLogContextBizType(LogBizTypeEnum bizType) {
        if (!LogBizTypeEnum.DEFAULT.equals(bizType)) {
            LogContext.set(LogScopeEnum.LOG_CONTEXT.getScope(), LogConstants.BIZ_TYPE, bizType.getType());
        }
    }

    public static void putDataSource() {
        LogContext.init(LogScopeEnum.DATA_SOURCE.getScope());
    }

    public static void removeDataSource() {
        LogContext.clear(LogScopeEnum.DATA_SOURCE.getScope());
    }

    public static boolean getDataSourceEnabled() {
        Boolean enabled = LogContext.get(LogScopeEnum.DATA_SOURCE.getScope(), LogConstants.ENABLED);
        return Boolean.TRUE.equals(enabled);
    }

    public static @Nullable List<String> getDataSourceKeyColumns() {
        return LogContext.get(LogScopeEnum.DATA_SOURCE.getScope(), LogConstants.KEY_COLUMNS);
    }

    public static @Nullable List<String> getDataSourceExcludeTableNames() {
        return LogContext.get(LogScopeEnum.DATA_SOURCE.getScope(), LogConstants.EXCLUDE_TABLE_NAMES);
    }

    public static @Nullable List<String> getDataSourceExcludeColumns() {
        return LogContext.get(LogScopeEnum.DATA_SOURCE.getScope(), LogConstants.EXCLUDE_COLUMNS);
    }

    public static void setDataSourceEnabled(boolean enabled) {
        LogContext.set(LogScopeEnum.DATA_SOURCE.getScope(), LogConstants.ENABLED, enabled);
    }

    public static void setDataSourceKeyColumns(List<String> keyColumns) {
        LogContext.set(LogScopeEnum.DATA_SOURCE.getScope(), LogConstants.KEY_COLUMNS, keyColumns);
    }

    public static void setDataSourceExcludeTableNames(List<String> excludeTableNames) {
        LogContext.set(LogScopeEnum.DATA_SOURCE.getScope(), LogConstants.EXCLUDE_TABLE_NAMES, excludeTableNames);
    }

    public static void setDataSourceExcludeColumns(List<String> excludeColumns) {
        LogContext.set(LogScopeEnum.DATA_SOURCE.getScope(), LogConstants.EXCLUDE_COLUMNS, excludeColumns);
    }
}