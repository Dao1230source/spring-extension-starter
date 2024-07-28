package org.source.spring.scan;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.type.classreading.MetadataReader;

import java.util.List;

public interface ScanProcessor {

    /**
     * 是否保留该class
     *
     * @param metadataReader metadataReader
     * @param clazz          clazz
     * @return boolean
     */
    boolean retainClass(@NotNull MetadataReader metadataReader, Class<?> clazz);

    /**
     * 处理保留的 classes
     *
     * @param classes classes
     */
    void processClasses(@NotNull List<Class<?>> classes);
}
