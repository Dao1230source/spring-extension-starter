package org.source.spring.log.processor;

import org.aopalliance.intercept.MethodInvocation;
import org.source.spring.common.spel.ExtendEvaluationContext;
import org.source.spring.common.spel.ExtendRootObject;
import org.source.spring.log.LogConstants;
import org.source.spring.log.LogContext;
import org.source.spring.log.Logs;
import org.source.spring.log.annotation.DataSourceLog;

import java.lang.reflect.Method;
import java.util.List;

public class DataSourceLogAnnoProcessor extends AbstractLogAnnotationProcessor<DataSourceLog, DataSourceLogAnnoProcessor> {

    @Override
    public DataSourceLog obtainAnnotation(MethodInvocation invocation) {
        return invocation.getMethod().getAnnotation(DataSourceLog.class);
    }

    @Override
    public void before(MethodDetail<DataSourceLog> detail) {
        LogContext.init(LogConstants.VARIABLES_DATA_SOURCE);
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
        LogContext.clear(LogConstants.VARIABLES_DATA_SOURCE);
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return method.isAnnotationPresent(DataSourceLog.class);
    }

}