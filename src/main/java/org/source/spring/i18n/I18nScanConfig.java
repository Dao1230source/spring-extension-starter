package org.source.spring.i18n;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.source.spring.i18n.annotation.I18nDict;
import org.source.spring.i18n.enums.I18nRefTypeEnum;
import org.source.spring.i18n.facade.data.Dict;
import org.source.spring.i18n.facade.data.EnumData;
import org.source.spring.scan.AbstractAnnotationScanProcessor;
import org.source.spring.scan.ExtendPackagesProcessor;
import org.source.spring.scan.ScanConfig;
import org.source.utility.utils.Strings;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@AutoConfiguration(before = {ScanConfig.class, I18nConfig.class})
@ConditionalOnProperty(prefix = "org.source.spring", name = "i18n", matchIfMissing = true)
public class I18nScanConfig {
    @Bean
    public I18nScanProcessor i18nScanProcessor() {
        return new I18nScanProcessor(List.of(I18nDict.class, I18nDict.List.class));
    }

    @EqualsAndHashCode(callSuper = false)
    @Getter
    public static class I18nScanProcessor extends AbstractAnnotationScanProcessor {
        private final List<Dict> dictList = new ArrayList<>();

        public I18nScanProcessor(List<Class<? extends Annotation>> annotationTypeList) {
            super(annotationTypeList);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void processClasses(@NotNull List<Class<?>> classes) {
            dictList.addAll(classes.stream().filter(Class::isEnum)
                    .map(k -> (Class<? extends Enum<?>>) k)
                    .flatMap(this::parse)
                    .flatMap(this::convert)
                    .toList());
        }

        private Stream<EnumData> parse(Class<? extends Enum<?>> enumClass) {
            Assert.isTrue(enumClass.isEnum(), Strings.format("class:{} must be a Enum class", enumClass.getName()));
            I18nDict[] i18nDict = enumClass.getAnnotationsByType(I18nDict.class);
            return Arrays.stream(i18nDict).map(k -> {
                EnumData enumData = new EnumData();
                enumData.setEnumClass(enumClass);
                enumData.setGroup(I18nRefTypeEnum.getI18nDictGroup(k, enumClass, i18nDict.length == 1));
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

    @Bean
    public ExtendPackagesProcessor i18n() {
        return new ExtendPackagesProcessor() {
            @Override
            public @NotNull List<String> extendPackages() {
                return List.of(ClassUtils.getPackageName(I18nTemplate.class.getName()));
            }

            @Override
            public @NotNull Class<?> groupClass() {
                return ScanConfig.class;
            }
        };
    }
}
