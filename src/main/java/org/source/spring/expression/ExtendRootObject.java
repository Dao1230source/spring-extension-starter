package org.source.spring.expression;

import lombok.Data;
import org.aopalliance.intercept.MethodInvocation;
import org.source.utility.constant.Constants;

import java.lang.reflect.Method;
import java.util.Objects;

@Data
public class ExtendRootObject {
    private final Method method;
    private final Object[] args;
    private final Object target;
    private final Class<?> targetClass;

    /*
     * 扩展属性
     */
    private Object methodResult;
    private String methodLocation;
    private Object param;
    private Object result;

    public ExtendRootObject(MethodInvocation invocation) {
        this.method = invocation.getMethod();
        this.args = invocation.getArguments();
        this.target = invocation.getThis();
        Class<?> cls = null;
        String clsName = "null";
        if (Objects.nonNull(this.target)) {
            cls = this.target.getClass();
            clsName = cls.getName();
        }
        this.targetClass = cls;
        this.methodLocation = clsName + Constants.HASH + method.getName();
    }
}
