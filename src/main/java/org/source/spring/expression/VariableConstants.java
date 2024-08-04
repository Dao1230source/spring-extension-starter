package org.source.spring.expression;

import lombok.experimental.UtilityClass;
import org.source.utility.constant.Constants;

@UtilityClass
public class VariableConstants {
    /**
     * spEl 变量名称
     */
    public static final String METHOD = "method";
    public static final String METHOD_NAME = "methodName";
    public static final String ARGS = "args";
    public static final String TARGET = "target";
    public static final String TARGET_CLASS = "targetClass";

    public static final String METHOD_RESULT = "methodResult";
    public static final String METHOD_LOCATION = "methodLocation";
    public static final String PARAM = "P";
    public static final String PARAM_SP_EL = Constants.HASH + PARAM;
    public static final String RESULT = "R";
    public static final String RESULT_SP_EL = Constants.HASH + RESULT;
}
