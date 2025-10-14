package org.source.spring.i18n.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.source.spring.i18n.I18nConstant;
import org.source.spring.i18n.I18nSerializer;

import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = I18nSerializer.class, nullsUsing = I18nSerializer.class)
public @interface I18n {

    String group() default I18nConstant.GROUP_DEFAULT;

    Class<?> groupClass() default Void.class;

    I18nRef key();
}
