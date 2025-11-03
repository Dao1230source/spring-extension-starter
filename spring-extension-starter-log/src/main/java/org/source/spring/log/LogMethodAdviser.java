package org.source.spring.log;

import org.source.spring.log.processor.AbstractLogAnnotationProcessor;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class LogMethodAdviser<A extends Annotation, P extends AbstractLogAnnotationProcessor<A, P>> extends StaticMethodMatcherPointcutAdvisor {
    private final transient LogMethodInterceptor<A, P> logMethodInterceptor;

    public LogMethodAdviser(LogMethodInterceptor<A, P> logMethodInterceptor) {
        super(logMethodInterceptor);
        this.logMethodInterceptor = logMethodInterceptor;
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return logMethodInterceptor.processor().matches(method, targetClass);
    }

    public static <A extends Annotation, P extends AbstractLogAnnotationProcessor<A, P>> LogMethodAdviser<A, P> of(P processor) {
        LogMethodAdviser<A, P> logMethodAdviser = new LogMethodAdviser<>(new LogMethodInterceptor<>(processor));
        logMethodAdviser.setOrder(processor.order());
        return logMethodAdviser;
    }

}