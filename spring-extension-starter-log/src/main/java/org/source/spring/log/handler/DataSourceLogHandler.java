package org.source.spring.log.handler;

import org.aopalliance.intercept.MethodInvocation;
import org.source.spring.common.spel.ExtendEvaluationContext;
import org.source.spring.common.spel.ExtendRootObject;
import org.source.spring.log.LogAnnotationHandler;
import org.source.spring.log.Logs;
import org.source.spring.log.annotation.DataSourceLog;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.reflect.Method;
import java.util.List;

@ConditionalOnProperty(prefix = "org.source.spring.enabled", name = "log", havingValue = "true", matchIfMissing = true)
@AutoConfiguration
public class DataSourceLogHandler extends LogAnnotationHandler<DataSourceLog, DataSourceLogHandler> {

    @Override
    public DataSourceLog obtainAnnotation(MethodInvocation invocation) {
        return invocation.getMethod().getAnnotation(DataSourceLog.class);
    }

    @Override
    public void before(MethodDetail<DataSourceLog> detail) {
        Logs.putDataSource();
        DataSourceLog dataSourceLog = detail.getAnnotation();
        Logs.setDataSourceEnabled(dataSourceLog.enabled());
        Logs.setDataSourceKeyColumns(List.of(dataSourceLog.keyColumns()));
        Logs.setDataSourceExcludeTableNames(List.of(dataSourceLog.excludeTableNames()));
        Logs.setDataSourceExcludeColumns(List.of(dataSourceLog.excludeColumns()));
        ExtendEvaluationContext<ExtendRootObject> context = detail.getEvaluationContext();
        Logs.setDataSourceParentLogId(LOG_EVALUATOR.parse(context, dataSourceLog.parentLogId(), String.class));
        Logs.setDataSourceRefId(LOG_EVALUATOR.parse(context, dataSourceLog.refId(), String.class));
    }

    @Override
    public void finals(MethodDetail<DataSourceLog> detail) {
        Logs.removeDataSource();
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return method.isAnnotationPresent(DataSourceLog.class);
    }

    @Override
    public DataSourceLogHandler getProcessor() {
        return this;
    }

}
