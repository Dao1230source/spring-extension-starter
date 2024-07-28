package org.source.spring.expression;

import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.expression.Expression;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ExtendExpressionEvaluator<T extends ExtendRootObject, E extends ExtendEvaluationContext<T>>
        extends CachedExpressionEvaluator {
    private final Map<ExpressionKey, Expression> keyCache = new ConcurrentHashMap<>(64);

    @SuppressWarnings("unchecked")
    public E createContext(MethodInvocation invocation) {
        return (E) new ExtendEvaluationContext<>(invocation, this.getParameterNameDiscoverer());
    }

    public <R> R parseThrow(E context, String expr, Class<R> parsedClass) {
        if (!StringUtils.hasText(expr)) {
            return null;
        }
        if (this.noNeedParse(context, expr)) {
            return null;
        }
        ExtendRootObject root = context.getRoot();
        AnnotatedElementKey elementKey = new AnnotatedElementKey(root.getMethod(), root.getTargetClass());
        Expression expression = this.getExpression(keyCache, elementKey, expr);
        return expression.getValue(context, parsedClass);
    }

    private boolean noNeedParse(E context, String spEl) {
        return spEl.contains(VariableConstants.RESULT_SP_EL) && !context.methodResultExists();
    }

    public <R> R parse(E context, String expr, Class<R> parsedClass) {
        try {
            return parseThrow(context, expr, parsedClass);
        } catch (Exception e) {
            log.error("parse expression exception", e);
            return null;
        }
    }

    public String replacePlaceHolder(String spEl, int idx) {
        spEl = spEl.replace(VariableConstants.PARAM_SP_EL, VariableConstants.PARAM_SP_EL + idx);
        spEl = spEl.replace(VariableConstants.RESULT_SP_EL, VariableConstants.RESULT_SP_EL + idx);
        return spEl;
    }
}
