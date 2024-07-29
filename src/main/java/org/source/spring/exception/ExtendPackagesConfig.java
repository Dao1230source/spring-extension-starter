package org.source.spring.exception;

import org.jetbrains.annotations.NotNull;
import org.source.spring.scan.ExtendPackagesProcessor;
import org.source.spring.scan.ScanConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.util.ClassUtils;

import java.util.List;

@ConditionalOnProperty(prefix = "org.source.spring.enabled", name = "exception", matchIfMissing = true)
@AutoConfigureBefore(ScanConfig.class)
@AutoConfiguration
public class ExtendPackagesConfig {

    @Bean
    public ExtendPackagesProcessor exceptions() {
        return new ExtendPackagesProcessor() {
            @Override
            public @NotNull List<String> extendPackages() {
                return List.of(ClassUtils.getPackageName(ExtendPackagesConfig.class.getName()));
            }

            @Override
            public @NotNull Class<?> groupClass() {
                return ScanConfig.class;
            }
        };
    }
}
