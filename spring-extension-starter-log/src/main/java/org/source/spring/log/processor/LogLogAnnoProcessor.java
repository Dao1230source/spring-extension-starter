package org.source.spring.log.processor;

import org.aopalliance.intercept.MethodInvocation;
import org.source.spring.log.LogData;
import org.source.spring.log.Logs;
import org.source.spring.log.annotation.Log;
import org.source.spring.log.enums.LogPrefixEnum;
import org.source.spring.log.enums.LogScopeEnum;
import org.source.spring.uid.UidPrefix;

import java.lang.reflect.Method;
import java.util.List;

public class LogLogAnnoProcessor extends AbstractLogAnnotationProcessor<Log> {

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return method.isAnnotationPresent(Log.class);
    }

    @Override
    public LogScopeEnum getLogScope() {
        return LogScopeEnum.LOG;
    }

    @Override
    public Log obtainAnnotation(MethodInvocation invocation) {
        return invocation.getMethod().getAnnotation(Log.class);
    }

    @Override
    public LogAnnoData obtainLogAnnoData(MethodDetail<Log> detail) {
        Log log = detail.getAnnotation();
        LogAnnoData logAnnoData = new LogAnnoData();
        logAnnoData.setBizId(log.bizId());
        logAnnoData.setParentBizId(log.parentBizId());
        logAnnoData.setRefBizId(log.refBizId());
        logAnnoData.setTitle(log.title());
        logAnnoData.setDesc(log.desc());
        logAnnoData.setSystemType(log.systemType());
        logAnnoData.setBizType(log.bizType());
        logAnnoData.setLogId(log.logId());
        logAnnoData.setUserId(log.userId());
        logAnnoData.setMethodLocation(log.methodLocation());
        logAnnoData.setParam(log.param());
        logAnnoData.setResult(log.result());
        logAnnoData.setExtra(log.extra());
        return logAnnoData;
    }

    @Override
    public UidPrefix getUidPrefix() {
        return LogPrefixEnum.LOG;
    }

    @Override
    public void finals(List<LogData> logData) {
        Logs.save(logData);
    }

    @Override
    public int order() {
        return 3;
    }
}