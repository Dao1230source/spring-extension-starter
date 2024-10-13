package org.source.spring.doc;

import org.source.spring.doc.processor.DefaultDocProcessor;
import org.source.spring.doc.processor.DocProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@ConditionalOnProperty(prefix = "org.source.spring.enabled", name = "doc", havingValue = "true", matchIfMissing = true)
@AutoConfiguration
public class DocConfig {

    @ConditionalOnMissingBean
    @Bean
    public DocProcessor docProcessor() {
        return new DefaultDocProcessor();
    }

}
