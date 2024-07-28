package org.source.spring.scan;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ScanProcessorResult {
    private final ScanProcessor processor;
    private final List<Class<?>> classes;

    public ScanProcessorResult(ScanProcessor processor) {
        this.processor = processor;
        classes = new ArrayList<>();
    }

    public void processClasses() {
        this.processor.processClasses(this.classes);
    }
}
