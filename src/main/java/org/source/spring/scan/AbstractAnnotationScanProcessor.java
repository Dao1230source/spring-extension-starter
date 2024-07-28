package org.source.spring.scan;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.source.utility.utils.Streams;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

@Data
public abstract class AbstractAnnotationScanProcessor implements ScanProcessor {
    private boolean checkField;
    private List<Class<? extends Annotation>> annotationTypeList;

    protected AbstractAnnotationScanProcessor(List<Class<? extends Annotation>> annotationTypeList) {
        this.annotationTypeList = annotationTypeList;
        this.checkField = false;
    }

    @Override
    public boolean retainClass(@NotNull MetadataReader metadataReader, Class<?> clazz) {
        AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
        if (Streams.of(this.annotationTypeList).anyMatch(k -> annotationMetadata.isAnnotated(k.getTypeName())
                || annotationMetadata.hasAnnotatedMethods(k.getTypeName()))) {
            return true;
        }
        return checkField && Arrays.stream(clazz.getDeclaredFields())
                .anyMatch(k -> Streams.of(this.annotationTypeList).anyMatch(k::isAnnotationPresent));
    }

}
