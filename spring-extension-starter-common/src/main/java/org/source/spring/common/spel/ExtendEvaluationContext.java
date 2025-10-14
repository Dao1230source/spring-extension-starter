package org.source.spring.common.spel;

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
        Object[] args = root.getArgs();
        String[] parameterNames = SpElUtil.getParameterNames(root.getMethod());
        if (Objects.nonNull(parameterNames) && parameterNames.length > args.length) {
            for (int i = 0; i < root.getArgs().length; i++) {
                super.setVariable(parameterNames[i], args[i]);
            }
        }
        super.setVariable(VariableConstants.METHOD_LOCATION, root.getMethodLocation());
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
