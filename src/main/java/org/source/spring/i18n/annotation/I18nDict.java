package org.source.spring.i18n.annotation;

import org.source.spring.i18n.enums.I18nRefTypeEnum;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(I18nDict.List.class)
public @interface I18nDict {

    I18nRef group() default @I18nRef(type = I18nRefTypeEnum.CLASS);

    I18nRef key() default @I18nRef(type = I18nRefTypeEnum.NAME);

    I18nRef value();

    @Documented
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        I18nDict[] value();
    }
}
