package org.source.spring.scan;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.source.utility.utils.Reflects;
import org.source.utility.utils.Streams;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@UtilityClass
@Slf4j
public class ScanUtil {

    private static final String CLASS_RESOURCE_PATTERN = "/**/*.class";

    /**
     * <pre>
     * 获取项目中指定注解的beans
     * 此方法适用于springboot尚未完全启动，ApplicationContextAware还未执行，ApplicationContext=null的情况下使用
     * </pre>
     * 一般作为组件jar依赖时 @AutoConfiguration 加载时，beanFactory 都为null
     *
     * @param processorList processorList
     * @return beans
     */
    public static List<ScanProcessorResult> scanPackages(Collection<String> packages,
                                                         Collection<ScanProcessor> processorList) {
        List<ScanProcessorResult> resultList = Streams.map(processorList, ScanProcessorResult::new).toList();
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        packages.forEach(p -> scanPackage(p, context, resultList));
        return resultList;
    }

    public static void scanPackage(String pkg,
                                   AnnotationConfigApplicationContext context,
                                   List<ScanProcessorResult> resultList) {
        String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                ClassUtils.convertClassNameToResourcePath(pkg) + CLASS_RESOURCE_PATTERN;
        try {
            Resource[] resources = context.getResources(pattern);
            MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(context);
            for (Resource resource : resources) {
                MetadataReader metadataReader = readerFactory.getMetadataReader(resource);
                String className = metadataReader.getClassMetadata().getClassName();
                Class<?> aClass = Reflects.classForNameOrDefault(className, null);
                if (Objects.isNull(aClass)) {
                    continue;
                }
                resultList.forEach(r -> {
                    if (r.getProcessor().retainClass(metadataReader, aClass)) {
                        r.getClasses().add(aClass);
                    }
                });
            }
        } catch (IOException e) {
            log.warn("SpringUtil.getClassNamesWithAnnotationByPackage exception, but ignore");
        }
    }

}
