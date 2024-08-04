package org.source.spring.log;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.source.spring.expression.ExtendEvaluationContext;
import org.source.spring.expression.ExtendExpressionEvaluator;
import org.source.spring.expression.ExtendRootObject;

@Slf4j
public class LogExpressionEvaluator extends ExtendExpressionEvaluator<ExtendRootObject, ExtendEvaluationContext<ExtendRootObject>> {

    @Override
    public ExtendEvaluationContext<ExtendRootObject> createContext(MethodInvocation invocation) {
        return new ExtendEvaluationContext<>(invocation, this.getParameterNameDiscoverer());
    }

}
