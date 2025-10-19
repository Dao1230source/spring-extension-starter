package org.source.spring.log.handler;

import org.aopalliance.intercept.MethodInvocation;
import org.source.spring.log.LogAnnotationHandler;
import org.source.spring.log.Logs;
import org.source.spring.log.annotation.LogContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.reflect.Method;
import java.util.Objects;

@ConditionalOnProperty(prefix = "org.source.spring.enabled", name = "log", havingValue = "true", matchIfMissing = true)
@AutoConfiguration
public class LogContextHandler extends LogAnnotationHandler<LogContext, LogContextHandler> {

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
        Logs.putLogContext();
        Logs.setLogContextSystemType(detail.getAnnotation().systemType());
        Logs.setLogContextBizType(detail.getAnnotation().bizType());
    }

    @Override
    public void finals(MethodDetail<LogContext> detail) {
        Logs.removeLogContext();
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return method.isAnnotationPresent(LogContext.class) || targetClass.isAnnotationPresent(LogContext.class);
    }

    @Override
    public LogContextHandler getProcessor() {
        return this;
    }

    @Override
    protected int order() {
        return 2;
    }
}
