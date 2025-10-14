package org.source.spring.common.utility;

import lombok.experimental.UtilityClass;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import java.util.Map;
import java.util.Objects;

@UtilityClass
public class EnableAnnotationUtil {
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

    /**
     * 使用该方法前请确保bean已经加载
     */
    public static <T> T getBeanFromRegistry(BeanDefinitionRegistry registry, Class<T> tClass) {
        if (registry instanceof ConfigurableListableBeanFactory) {
            return ((ConfigurableListableBeanFactory) registry).getBean(tClass);
        } else {
            throw new IllegalStateException("BeanDefinitionRegistry must be a ConfigurableListableBeanFactory");
        }
    }
}
