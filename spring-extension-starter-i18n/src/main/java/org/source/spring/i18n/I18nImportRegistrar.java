package org.source.spring.i18n;

import lombok.extern.slf4j.Slf4j;
import org.source.spring.common.AbstractImportRegistrar;
import org.source.spring.common.exception.SpExtExceptionEnum;
import org.source.spring.common.utility.ImportRegistrarUtil;
import org.source.spring.i18n.annotation.I18nServer;
import org.source.spring.i18n.enums.I18nRefTypeEnum;
import org.source.spring.i18n.facade.data.DictData;
import org.source.spring.i18n.facade.data.EnumData;
import org.source.spring.i18n.facade.param.Dict4Param;
import org.source.spring.i18n.processor.DefaultI18nProcessor;
import org.source.spring.i18n.processor.I18nProcessor;
import org.source.utility.utils.Streams;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Slf4j
public class I18nImportRegistrar extends AbstractImportRegistrar {

    @SuppressWarnings("unchecked")
    @Override
    public void registerBeanDefinitionsAfterContainerReady(AnnotationMetadata importingClassMetadata,
                                                           BeanDefinitionRegistry registry,
                                                           ApplicationContext applicationContext) {
        log.debug("Register bean definitions for @EnableExtendedI18n");
        // 创建自定义扫描器，并关闭默认过滤器（这样只会扫描我们指定的注解）
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        // 添加包含过滤器，只扫描带有注解的类
        scanner.addIncludeFilter(new AnnotationTypeFilter(I18nServer.class));
        I18nTemplate i18nTemplate = new I18nTemplate(this.obtainI18nProcessor(applicationContext));
        I18nWrapper.setI18nTemplate(i18nTemplate);
        ImportRegistrarUtil.registerBeanDefinition(registry, I18nTemplate.class, () -> i18nTemplate);
        String[] basePackages = ImportRegistrarUtil.getBasePackages(importingClassMetadata, EnableExtendedI18n.class.getName());
        List<Dict4Param> dictList = Streams.of(basePackages)
                .map(basePackage -> Streams.of(scanner.findCandidateComponents(basePackage))
                        .filter(k -> StringUtils.hasText(k.getBeanClassName()))
                        .map(k -> ClassUtils.resolveClassName(k.getBeanClassName(), this.resourceLoader.getClassLoader())).toList())
                .flatMap(Collection::stream)
                .filter(Class::isEnum)
                .map(k -> (Class<? extends Enum<?>>) k)
                .flatMap(this::parse)
                .flatMap(this::convert)
                .map(Dict4Param::new)
                .toList();
        int savedSize = I18nWrapper.save(dictList);
        log.debug("Saved {} dict", savedSize);
    }

    private I18nProcessor obtainI18nProcessor(ApplicationContext applicationContext) {
        try {
            return applicationContext.getBean(I18nProcessor.class);
        } catch (BeansException e) {
            return new DefaultI18nProcessor();
        }
    }

    private Stream<EnumData> parse(Class<? extends Enum<?>> enumClass) {
        SpExtExceptionEnum.IS_NOT_AN_ENUM.isTrue(enumClass.isEnum(), "class:{} must be a Enum class", enumClass.getName());
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

    private Stream<DictData> convert(EnumData enumData) {
        return Arrays.stream(enumData.getEnumClass().getEnumConstants()).map(e -> {
            DictData dictData = new DictData();
            dictData.setGroup(enumData.getGroup());
            dictData.setKey(I18nRefTypeEnum.getValue(enumData.getKey(), e));
            dictData.setValue(I18nRefTypeEnum.getValue(enumData.getValue(), e));
            dictData.setScope(Locale.getDefault().toLanguageTag());
            return dictData;
        });
    }
}
