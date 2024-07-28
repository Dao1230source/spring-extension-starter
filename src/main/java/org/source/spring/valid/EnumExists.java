package org.source.spring.valid;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.source.utility.enums.BaseExceptionEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


/**
 * @author zengfugen
 */
@Documented
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Repeatable(EnumExists.List.class)
@Constraint(validatedBy = {EnumExists.EnumValidator.class})
public @interface EnumExists {
    String message() default "{*.validation.constraint.Enum.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * 枚举类
     *
     * @return enum Class
     */
    Class<?> clazz();

    /**
     * 方法名
     *
     * @return method's name
     */
    String method();


    /**
     * Defines several {@link EnumExists} annotations on the same element.
     *
     * @see EnumExists
     */
    @Documented
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
    @Retention(RUNTIME)
    @interface List {
        EnumExists[] value();
    }


    class EnumValidator implements ConstraintValidator<EnumExists, Object> {

        private EnumExists annotation;

        @Override
        public void initialize(EnumExists constraintAnnotation) {
            this.annotation = constraintAnnotation;
        }

        @Override
        public boolean isValid(Object value, ConstraintValidatorContext context) {
            // 为空不校验
            if (value == null) {
                return true;
            }
            if (!annotation.clazz().isEnum()) {
                return true;
            }
            Object[] objects = annotation.clazz().getEnumConstants();
            try {
                Method method = annotation.clazz().getMethod(annotation.method());
                for (Object o : objects) {
                    if (value.equals(method.invoke(o))) {
                        return true;
                    }
                }
            } catch (Exception e) {
                throw BaseExceptionEnum.ENUM_EXCEPTION.except(e);
            }
            return false;
        }
    }
}
