package org.source.spring.log;

import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class LogMethodAdviser<A extends Annotation, P extends LogAnnotationHandler<A, P>> extends StaticMethodMatcherPointcutAdvisor {
    private final transient LogMethodInterceptor<A, P> logMethodInterceptor;

    public LogMethodAdviser(LogMethodInterceptor<A, P> logMethodInterceptor) {
        super(logMethodInterceptor);
        this.logMethodInterceptor = logMethodInterceptor;
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return logMethodInterceptor.getProcessor().matches(method, targetClass);
    }

}
