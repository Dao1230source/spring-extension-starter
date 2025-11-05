package org.source.spring.log.processor;

import org.aopalliance.intercept.MethodInvocation;
import org.source.spring.log.Logs;
import org.source.spring.log.annotation.LogContext;
import org.source.spring.log.enums.LogScopeEnum;

import java.lang.reflect.Method;
import java.util.Objects;

public class LogContextLogAnnoProcessor extends AbstractLogAnnotationProcessor<LogContext> {

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return method.isAnnotationPresent(LogContext.class) || targetClass.isAnnotationPresent(LogContext.class);
    }

    @Override
    public LogScopeEnum getLogScope() {
        return LogScopeEnum.LOG_CONTEXT;
    }

    @Override
    public LogContext obtainAnnotation(MethodInvocation invocation) {
        LogContext logContext = invocation.getMethod().getAnnotation(LogContext.class);
        Object target = invocation.getThis();
        if (Objects.isNull(logContext) && Objects.nonNull(target)) {
            logContext = target.getClass().getAnnotation(LogContext.class);
        }
        return logContext;
    }

    @Override
    public void before(MethodDetail<LogContext> detail) {
        Logs.setLogContextSystemType(detail.getAnnotation().systemType());
        Logs.setLogContextBizType(detail.getAnnotation().bizType());
    }

    @Override
    public int order() {
        return 2;
    }
}