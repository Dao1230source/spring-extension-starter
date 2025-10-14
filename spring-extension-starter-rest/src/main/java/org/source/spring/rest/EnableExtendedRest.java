package org.source.spring.rest;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({RestImportRegistrar.class})
public @interface EnableExtendedRest {
    String[] basePackages() default {};
}
