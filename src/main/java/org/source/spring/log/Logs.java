package org.source.spring.log;

import lombok.extern.slf4j.Slf4j;
import org.source.spring.log.enums.LogBizTypeEnum;
import org.source.spring.log.enums.LogSystemTypeEnum;
import org.source.spring.trace.TraceContext;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Slf4j
public class Logs {
    private static LogDataProcessor logDataProcessor;

    public static synchronized void setLogDataProcessor(LogDataProcessor logDataProcessor) {
        Logs.logDataProcessor = logDataProcessor;
    }

    public static void save(List<LogData> logDataList) {
        logDataProcessor.save(logDataList);
    }

    public static void save(LogData logData) {
        logDataProcessor.save(List.of(logData));
    }

    public static void put(String scopeName) {
        LogContext.putEmpty(scopeName);
    }

    public static void put(String scopeName, String k, Object v) {
        LogContext.set(scopeName, k, v);
    }

    public static Object get(String scopeName, String k) {
        return LogContext.get(scopeName, k);
    }

    public static void remove(String scopeName) {
        LogContext.remove(scopeName);
    }

    public static void clear() {
        LogContext.clear();
    }

    public static void putLog() {
        LogContext.putEmpty(NameConstants.VARIABLES_LOG);
    }

    public static void removeLog() {
        LogContext.remove(NameConstants.VARIABLES_LOG);
    }

    public static String getLogId() {
        return (String) LogContext.get(NameConstants.VARIABLES_LOG, NameConstants.LOG_ID);
    }

    public static void setLogId(String logId) {
        LogContext.set(NameConstants.VARIABLES_LOG, NameConstants.LOG_ID, logId);
    }

    public static String getRefId() {
        return (String) LogContext.get(NameConstants.VARIABLES_LOG, NameConstants.REF_ID);
    }

    public static void setRefId(String refId) {
        LogContext.set(NameConstants.VARIABLES_LOG, NameConstants.REF_ID, refId);
    }

    public static String getDesc() {
        return (String) LogContext.get(NameConstants.VARIABLES_LOG, NameConstants.DESC);
    }

    public static void setDesc(String desc) {
        LogContext.set(NameConstants.VARIABLES_LOG, NameConstants.DESC, desc);
    }

    public static Integer getSystemType() {
        Integer systemType = (Integer) LogContext.get(NameConstants.VARIABLES_LOG, NameConstants.SYSTEM_TYPE);
        if (Objects.isNull(systemType) || LogSystemTypeEnum.DEFAULT.getType().equals(systemType)) {
            systemType = (Integer) LogContext.searchAll(NameConstants.VARIABLES_LOG_CONTEXT, NameConstants.SYSTEM_TYPE);
        }
        if (Objects.isNull(systemType)) {
            systemType = LogSystemTypeEnum.DEFAULT.getType();
        }
        return systemType;
    }

    public static Integer getSystemType(LogSystemTypeEnum systemType) {
        // 非默认值，直接返回
        if (Objects.nonNull(systemType) && !LogSystemTypeEnum.DEFAULT.equals(systemType)) {
            return systemType.getType();
        }
        return getSystemType();
    }

    public static void setSystemType(LogSystemTypeEnum systemType) {
        if (Objects.nonNull(systemType) && !LogSystemTypeEnum.DEFAULT.equals(systemType)) {
            LogContext.set(NameConstants.VARIABLES_LOG, NameConstants.SYSTEM_TYPE, systemType.getType());
        }
    }

    public static String getParentLogId() {
        return (String) LogContext.get(NameConstants.VARIABLES_LOG, NameConstants.PARENT_LOG_ID);
    }

    public static void setParentLogId(String parentLogId) {
        LogContext.set(NameConstants.VARIABLES_LOG, NameConstants.PARENT_LOG_ID, parentLogId);
    }

    public static String getTitle() {
        return (String) LogContext.get(NameConstants.VARIABLES_LOG, NameConstants.TITLE);
    }

    public static void setTitle(String title) {
        LogContext.set(NameConstants.VARIABLES_LOG, NameConstants.TITLE, title);
    }

    public static String getUserId() {
        return (String) LogContext.get(NameConstants.VARIABLES_LOG, NameConstants.USER_ID);
    }

    public static void setUserId(String userId) {
        LogContext.set(NameConstants.VARIABLES_LOG, NameConstants.USER_ID, userId);
    }

    public static Integer getBizType() {
        Integer bizType = (Integer) LogContext.get(NameConstants.VARIABLES_LOG, NameConstants.BIZ_TYPE);
        if (Objects.isNull(bizType) || LogBizTypeEnum.DEFAULT.getType().equals(bizType)) {
            bizType = (Integer) LogContext.searchAll(NameConstants.VARIABLES_LOG_CONTEXT, NameConstants.BIZ_TYPE);
        }
        if (Objects.isNull(bizType)) {
            bizType = LogBizTypeEnum.DEFAULT.getType();
        }
        return bizType;
    }

    public static Integer getBizType(LogBizTypeEnum bizType) {
        if (Objects.nonNull(bizType) && !LogBizTypeEnum.DEFAULT.equals(bizType)) {
            return bizType.getType();
        }
        return getBizType();
    }

    public static void setBizType(LogBizTypeEnum bizType) {
        if (Objects.nonNull(bizType) && !LogBizTypeEnum.DEFAULT.equals(bizType)) {
            LogContext.set(NameConstants.VARIABLES_LOG, NameConstants.BIZ_TYPE, bizType.getType());
        }
    }

    public static Object getParam() {
        return LogContext.get(NameConstants.VARIABLES_LOG, NameConstants.PARAM);
    }

    public static void setParam(Object param) {
        LogContext.set(NameConstants.VARIABLES_LOG, NameConstants.PARAM, param);
    }

    public static Object getResult() {
        return LogContext.get(NameConstants.VARIABLES_LOG, NameConstants.RESULT);
    }

    public static void setResult(Object result) {
        LogContext.set(NameConstants.VARIABLES_LOG, NameConstants.RESULT, result);
    }

    public static Object getExtra() {
        return LogContext.get(NameConstants.VARIABLES_LOG, NameConstants.EXTRA);
    }

    public static void setExtra(Object extra) {
        LogContext.set(NameConstants.VARIABLES_LOG, NameConstants.EXTRA, extra);
    }

    public static String getMethodLocation() {
        return (String) LogContext.get(NameConstants.VARIABLES_LOG, NameConstants.METHOD_LOCATION);
    }

    public static void setMethodLocation(String methodLocation) {
        LogContext.set(NameConstants.VARIABLES_LOG, NameConstants.METHOD_LOCATION, methodLocation);
    }

    public static void putLogContext() {
        LogContext.putEmpty(NameConstants.VARIABLES_LOG_CONTEXT);
    }

    public static void removeLogContext() {
        LogContext.remove(NameConstants.VARIABLES_LOG_CONTEXT);
    }

    public static void setLogContextSystemType(LogSystemTypeEnum systemType) {
        if (Objects.nonNull(systemType) && !LogSystemTypeEnum.DEFAULT.equals(systemType)) {
            LogContext.set(NameConstants.VARIABLES_LOG_CONTEXT, NameConstants.SYSTEM_TYPE, systemType.getType());
        }
    }

    public static void setLogContextBizType(LogBizTypeEnum bizType) {
        if (Objects.nonNull(bizType) && !LogBizTypeEnum.DEFAULT.equals(bizType)) {
            LogContext.set(NameConstants.VARIABLES_LOG_CONTEXT, NameConstants.BIZ_TYPE, bizType.getType());
        }
    }

    public static void putDataSource() {
        LogContext.putEmpty(NameConstants.VARIABLES_DATA_SOURCE);
    }

    public static void removeDataSource() {
        LogContext.remove(NameConstants.VARIABLES_DATA_SOURCE);
    }

    public static boolean getDataSourceEnabled() {
        return (boolean) LogContext.get(NameConstants.VARIABLES_DATA_SOURCE, NameConstants.ENABLED);
    }

    public static String getDataSourceParentLogId() {
        String parentLogId = (String) LogContext.get(NameConstants.VARIABLES_DATA_SOURCE, NameConstants.PARENT_LOG_ID);
        if (!StringUtils.hasText(parentLogId)) {
            parentLogId = TraceContext.getTraceId();
        }
        return parentLogId;
    }

    public static String getDataSourceRefId() {
        String refId = (String) LogContext.get(NameConstants.VARIABLES_DATA_SOURCE, NameConstants.REF_ID);
        if (!StringUtils.hasText(refId)) {
            refId = TraceContext.getTraceId();
        }
        return refId;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getDataSourceKeyColumns() {
        return (List<String>) LogContext.get(NameConstants.VARIABLES_DATA_SOURCE, NameConstants.KEY_COLUMNS);
    }

    @SuppressWarnings("unchecked")
    public static List<String> getDataSourceExcludeTableNames() {
        return (List<String>) LogContext.get(NameConstants.VARIABLES_DATA_SOURCE, NameConstants.EXCLUDE_TABLE_NAMES);
    }

    @SuppressWarnings("unchecked")
    public static List<String> getDataSourceExcludeColumns() {
        return (List<String>) LogContext.get(NameConstants.VARIABLES_DATA_SOURCE, NameConstants.EXCLUDE_COLUMNS);
    }

    public static void setDataSourceEnabled(boolean enabled) {
        LogContext.set(NameConstants.VARIABLES_DATA_SOURCE, NameConstants.ENABLED, enabled);
    }

    public static void setDataSourceKeyColumns(List<String> keyColumns) {
        LogContext.set(NameConstants.VARIABLES_DATA_SOURCE, NameConstants.KEY_COLUMNS, keyColumns);
    }

    public static void setDataSourceExcludeTableNames(List<String> excludeTableNames) {
        LogContext.set(NameConstants.VARIABLES_DATA_SOURCE, NameConstants.EXCLUDE_TABLE_NAMES, excludeTableNames);
    }

    public static void setDataSourceExcludeColumns(List<String> excludeColumns) {
        LogContext.set(NameConstants.VARIABLES_DATA_SOURCE, NameConstants.EXCLUDE_COLUMNS, excludeColumns);
    }

    public static void setDataSourceParentLogId(String parentLogId) {
        LogContext.set(NameConstants.VARIABLES_DATA_SOURCE, NameConstants.PARENT_LOG_ID, parentLogId);
    }

    public static void setDataSourceRefId(String refId) {
        LogContext.set(NameConstants.VARIABLES_DATA_SOURCE, NameConstants.REF_ID, refId);
    }
}
