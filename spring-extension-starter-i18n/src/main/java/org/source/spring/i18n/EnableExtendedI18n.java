package org.source.spring.i18n;

import org.source.spring.cache.EnableExtendedCache;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnableExtendedCache
@Import({I18nImportRegistrar.class})
public @interface EnableExtendedI18n {
    String[] basePackages() default {};
}
