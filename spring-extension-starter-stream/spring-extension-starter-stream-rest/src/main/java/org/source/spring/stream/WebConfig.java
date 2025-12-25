package org.source.spring.stream;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

@AutoConfiguration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public UrlPathHelper urlPathHelper() {
        UrlPathHelper urlPathHelper = new UrlPathHelper();
        urlPathHelper.setRemoveSemicolonContent(false);
        urlPathHelper.setUrlDecode(true);
        return urlPathHelper;
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUrlPathHelper(urlPathHelper());
    }
}