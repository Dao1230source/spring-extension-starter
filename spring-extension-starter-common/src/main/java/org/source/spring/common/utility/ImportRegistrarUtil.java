package org.source.spring.common.utility;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.source.utility.constant.Constants;
import org.source.utility.utils.Strings;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

@Slf4j
@UtilityClass
public class ImportRegistrarUtil {
    public static final String BASE_PACKAGES_ATTRIBUTE_NAME = "basePackages";

    public static String[] getBasePackages(AnnotationMetadata metadata, String annotationClassName, String basePackageAttributeName) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(annotationClassName);
        if (Objects.isNull(attributes)) {
            return new String[0];
        }
        String[] basePackages = (String[]) attributes.get(basePackageAttributeName);
        if (basePackages == null || basePackages.length == 0) {
            String className = metadata.getClassName();
            return new String[]{ClassUtils.getPackageName(className)};
        }
        return basePackages;
    }

    public static String[] getBasePackages(AnnotationMetadata metadata, String annotationClassName) {
        return getBasePackages(metadata, annotationClassName, BASE_PACKAGES_ATTRIBUTE_NAME);
    }

    public void registerBeanDefinition(BeanDefinitionRegistry registry, Class<?> eClass, Supplier<?> instance) {
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(eClass);
        beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
        beanDefinition.setRole(BeanDefinition.ROLE_SUPPORT);
        beanDefinition.setInstanceSupplier(instance);
        String beanName = Strings.removePrefixAndLowerFirst(eClass.getSimpleName(), Constants.EMPTY);
        log.debug("registerBeanDefinition:{}", beanName);
        registry.registerBeanDefinition(beanName, beanDefinition);
    }
}