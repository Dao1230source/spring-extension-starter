package org.source.spring.i18n.annotation;

import org.source.spring.i18n.enums.I18nRefTypeEnum;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface I18nRef {
    I18nRefTypeEnum type() default I18nRefTypeEnum.SELF;

    String value() default "";
}
