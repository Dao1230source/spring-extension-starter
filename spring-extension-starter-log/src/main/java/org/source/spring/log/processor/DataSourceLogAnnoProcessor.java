package org.source.spring.log.processor;

import org.aopalliance.intercept.MethodInvocation;
import org.source.spring.log.LogData;
import org.source.spring.log.Logs;
import org.source.spring.log.annotation.DataSourceLog;
import org.source.spring.log.enums.LogPrefixEnum;
import org.source.spring.log.enums.LogScopeEnum;
import org.source.spring.uid.UidPrefix;
import org.source.spring.uid.Uids;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.List;

public class DataSourceLogAnnoProcessor extends AbstractLogAnnotationProcessor<DataSourceLog> {

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return method.isAnnotationPresent(DataSourceLog.class);
    }

    @Override
    public LogScopeEnum getLogScope() {
        return LogScopeEnum.DATA_SOURCE;
    }

    @Override
    public DataSourceLog obtainAnnotation(MethodInvocation invocation) {
        return invocation.getMethod().getAnnotation(DataSourceLog.class);
    }

    @Override
    public LogAnnoData obtainLogAnnoData(MethodDetail<DataSourceLog> detail) {
        DataSourceLog dataSourceLog = detail.getAnnotation();
        LogAnnoData logAnnoData = new LogAnnoData();
        logAnnoData.setRefBizId(dataSourceLog.refBizId());
        // 由于数据库变更日志拆分到字段级别，一条sql变更会有多个变更日志对象，用refBizId统一标记，表示属于同一次变更
        if (!StringUtils.hasText(dataSourceLog.refBizId())) {
            Logs.setRefBizId(this.getLogScope(), Uids.stringId(this.getUidPrefix()));
        }
        logAnnoData.setTitle(dataSourceLog.title());
        logAnnoData.setDesc(dataSourceLog.desc());
        logAnnoData.setSystemType(dataSourceLog.systemType());
        logAnnoData.setBizType(dataSourceLog.bizType());
        return logAnnoData;
    }

    @Override
    public UidPrefix getUidPrefix() {
        return LogPrefixEnum.LOG_DATASOURCE;
    }

    @Override
    public void before(MethodDetail<DataSourceLog> detail) {
        DataSourceLog dataSourceLog = detail.getAnnotation();
        Logs.setDataSourceEnabled(dataSourceLog.enabled());
        Logs.setDataSourceKeyColumns(List.of(dataSourceLog.keyColumns()));
        Logs.setDataSourceExcludeTableNames(List.of(dataSourceLog.excludeTableNames()));
        Logs.setDataSourceExcludeColumns(List.of(dataSourceLog.excludeColumns()));
    }

    @Override
    public void finals(List<LogData> logData) {
        Logs.save(logData);
    }
}