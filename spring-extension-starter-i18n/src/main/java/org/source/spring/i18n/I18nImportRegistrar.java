package org.source.spring.i18n;

import lombok.extern.slf4j.Slf4j;
import org.source.spring.common.utility.EnableAnnotationUtil;
import org.source.spring.i18n.annotation.I18nServer;
import org.source.spring.i18n.enums.I18nRefTypeEnum;
import org.source.spring.i18n.facade.data.Dict;
import org.source.spring.i18n.facade.data.EnumData;
import org.source.utility.utils.Reflects;
import org.source.utility.utils.Streams;
import org.source.utility.utils.Strings;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Stream;

@Slf4j
public class I18nImportRegistrar implements ImportBeanDefinitionRegistrar {

    @SuppressWarnings("unchecked")
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        log.debug("Register bean definitions for @EnableExtendedI18n");
        // 创建自定义扫描器，并关闭默认过滤器（这样只会扫描我们指定的注解）
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        // 添加包含过滤器，只扫描带有注解的类
        scanner.addIncludeFilter(new AnnotationTypeFilter(I18nServer.class));
        String[] basePackages = EnableAnnotationUtil.getBasePackages(importingClassMetadata, EnableExtendedI18n.class.getName());
        Streams.of(basePackages)
                .map(basePackage -> Streams.of(scanner.findCandidateComponents(basePackage))
                        .map(beanDefinition -> Reflects.classForName(beanDefinition.getBeanClassName())).toList())
                .flatMap(Collection::stream)
                .filter(Class::isEnum)
                .map(k -> (Class<? extends Enum<?>>) k)
                .flatMap(this::parse)
                .flatMap(this::convert)
                .toList();
    }

    private Stream<EnumData> parse(Class<? extends Enum<?>> enumClass) {
        Assert.isTrue(enumClass.isEnum(), Strings.format("class:{} must be a Enum class", enumClass.getName()));
        I18nServer[] i18NServer = enumClass.getAnnotationsByType(I18nServer.class);
        return Arrays.stream(i18NServer).map(k -> {
            EnumData enumData = new EnumData();
            enumData.setEnumClass(enumClass);
            enumData.setGroup(I18nRefTypeEnum.getI18nDictGroup(k, enumClass, i18NServer.length == 1));
            enumData.setKey(k.key());
            enumData.setValue(k.value());
            return enumData;
        });
    }

    private Stream<Dict> convert(EnumData enumData) {
        return Arrays.stream(enumData.getEnumClass().getEnumConstants()).map(e -> {
            Dict dict = new Dict();
            dict.setGroup(enumData.getGroup());
            dict.setKey(I18nRefTypeEnum.getValue(enumData.getKey(), e));
            dict.setValue(I18nRefTypeEnum.getValue(enumData.getValue(), e));
            dict.setScope(Locale.getDefault().toLanguageTag());
            return dict;
        });
    }
}
