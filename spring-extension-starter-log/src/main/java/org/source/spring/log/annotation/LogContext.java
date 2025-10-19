package org.source.spring.log.annotation;

import org.source.spring.log.enums.LogBizTypeEnum;
import org.source.spring.log.enums.LogSystemTypeEnum;

import java.lang.annotation.*;

@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogContext {

    /**
     * 系统代码层次的分类 {@link LogSystemTypeEnum}
     */
    LogSystemTypeEnum systemType() default LogSystemTypeEnum.DEFAULT;

    /**
     * 实际业务层级的分类 {@link LogBizTypeEnum}
     */
    LogBizTypeEnum bizType() default LogBizTypeEnum.DEFAULT;
}
