package org.source.spring.log;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;

@Slf4j
@Getter
@AllArgsConstructor
public class LogMethodInterceptor<A extends Annotation, P extends LogAnnotationHandler<A, P>> implements MethodInterceptor {
    private final P processor;

    @Nullable
    @Override
    public Object invoke(@NotNull MethodInvocation invocation) throws Throwable {
        log.debug("processor:{}, method:{}", processor.getClass().getSimpleName(), invocation.getMethod().getName());
        LogAnnotationHandler.MethodDetail<A> detail = processor.load(invocation);
        try {
            processor.doBefore(detail);
            Object result = invocation.proceed();
            processor.doAfter(detail, result);
            return result;
        } catch (Exception ex) {
            processor.doException(detail, ex);
            throw ex;
        } finally {
            try {
                processor.doFinal(detail);
            } catch (Exception ex) {
                log.error("处理日志逻辑异常", ex);
            }
        }
    }
}
