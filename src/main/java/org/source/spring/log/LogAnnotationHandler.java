package org.source.spring.log;

import lombok.Data;
import lombok.Getter;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.source.spring.expression.ExtendEvaluationContext;
import org.source.spring.expression.ExtendRootObject;
import org.springframework.core.Ordered;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Getter
public abstract class LogAnnotationHandler<A extends Annotation, P extends LogAnnotationHandler<A, P>> implements Serializable {
    protected static final LogExpressionEvaluator LOG_EVALUATOR = new LogExpressionEvaluator();

    @Data
    public static class MethodDetail<A> {
        protected MethodInvocation invocation;
        protected LogData logData;
        protected ExtendEvaluationContext<ExtendRootObject> evaluationContext;
        protected A annotation;
    }

    public MethodDetail<A> load(MethodInvocation invocation) {
        MethodDetail<A> detail = new MethodDetail<>();
        detail.invocation = invocation;
        detail.logData = new LogData();
        detail.evaluationContext = LOG_EVALUATOR.createContext(invocation);
        detail.annotation = obtainAnnotation(invocation);
        return detail;
    }

    public abstract void before(MethodDetail<A> detail);

    public void after(MethodDetail<A> detail) {
    }

    public void exception(MethodDetail<A> detail) {
    }

    public abstract void finals(MethodDetail<A> detail);

    public abstract boolean matches(@NotNull Method method, @NotNull Class<?> targetClass);

    public abstract P getProcessor();

    @Nullable
    public A obtainAnnotation(MethodInvocation invocation) {
        return null;
    }

    public void doBefore(MethodDetail<A> detail) {
        detail.logData.setStartTime(LocalDateTime.now());
        this.before(detail);
    }

    public void doAfter(MethodDetail<A> detail, Object result) {
        detail.logData.setEndTime(LocalDateTime.now());
        detail.evaluationContext.setMethodResult(result);
        this.after(detail);
    }

    public void doException(MethodDetail<A> detail, Exception ex) {
        String stackTrace = ExceptionUtils.getStackTrace(ex);
        String exceptionMessage = stackTrace.substring(0, Math.min(1000, stackTrace.length()));
        detail.logData.setExceptionMessage(exceptionMessage);
        this.exception(detail);
    }

    public void doFinal(MethodDetail<A> detail) {
        this.finals(detail);
    }

    protected int order() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    public LogMethodAdviser<A, P> createMethodAdviser() {
        LogMethodAdviser<A, P> logMethodAdviser = new LogMethodAdviser<>(new LogMethodInterceptor<>(this.getProcessor()));
        logMethodAdviser.setOrder(this.order());
        return logMethodAdviser;
    }
}
