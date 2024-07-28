package org.source.spring.expression;

import lombok.Getter;
import lombok.Setter;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;

import java.util.Collection;
import java.util.Objects;

@Getter
@Setter
public class ExtendEvaluationContext<T extends ExtendRootObject> extends MethodBasedEvaluationContext {
    private final T root;

    public ExtendEvaluationContext(T root, ParameterNameDiscoverer discoverer) {
        super(root, root.getMethod(), root.getArgs(), discoverer);
        this.root = root;
    }

    @SuppressWarnings("unchecked")
    public ExtendEvaluationContext(MethodInvocation invocation, ParameterNameDiscoverer discoverer) {
        this((T) new ExtendRootObject(invocation), discoverer);
    }

    @Override
    protected void lazyLoadArguments() {
        super.lazyLoadArguments();
        super.setVariable(VariableConstants.METHOD, root.getMethod());
        super.setVariable(VariableConstants.METHOD_NAME, root.getMethod().getName());
        super.setVariable(VariableConstants.ARGS, root.getArgs());
        super.setVariable(VariableConstants.TARGET, root.getTarget());
        super.setVariable(VariableConstants.TARGET_CLASS, root.getTargetClass());
    }

    public void setMethodResult(Object methodResult) {
        this.root.setMethodResult(methodResult);
        if (Objects.nonNull(methodResult)) {
            super.setVariable(VariableConstants.METHOD_RESULT, methodResult);
        }
    }

    public void setParam(Object param) {
        this.root.setParam(param);
        if (Objects.nonNull(param)) {
            this.putVariable(VariableConstants.PARAM, param);
        }
    }

    public void setResult(Object result) {
        this.root.setResult(result);
        if (Objects.nonNull(result)) {
            this.putVariable(VariableConstants.RESULT, result);
        }
    }

    private void putVariable(String name, Object obj) {
        super.setVariable(name, obj);
        if (obj instanceof Collection<?> os) {
            int i = 0;
            for (Object p : os) {
                super.setVariable(name + i, p);
                i += 1;
            }
        }
    }

    public boolean methodResultExists() {
        return Objects.nonNull(this.lookupVariable(VariableConstants.METHOD_RESULT));
    }
}
