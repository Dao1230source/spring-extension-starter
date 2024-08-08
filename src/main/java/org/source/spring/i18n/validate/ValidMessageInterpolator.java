package org.source.spring.i18n.validate;

import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;
import org.source.spring.i18n.annotation.I18nIgnore;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

public class ValidMessageInterpolator extends ResourceBundleMessageInterpolator {
    private final ResourceBundleLocator resourceBundleLocator;

    public ValidMessageInterpolator(ResourceBundleLocator userResourceBundleLocator) {
        super(userResourceBundleLocator);
        this.resourceBundleLocator = userResourceBundleLocator;
    }

    @Override
    public String interpolate(String message, Context context) {
        return this.interpolate(message, context, LocaleContextHolder.getLocale());
    }

    @Override
    public String interpolate(String message, Context context, Locale locale) {
        String i18nMessage = null;
        // 被校验的值为空或没有 I18nIgnore 注解
        if (Objects.isNull(context.getValidatedValue())
                || !context.getValidatedValue().getClass().isAnnotationPresent(I18nIgnore.class)) {
            // 通过 DictMessageSource 获取国际化内容
            ResourceBundle resourceBundle = resourceBundleLocator.getResourceBundle(locale);
            i18nMessage = resourceBundle.getString(message);
        }
        if (StringUtils.hasText(i18nMessage)) {
            message = i18nMessage;
        }
        return super.interpolate(message, context, locale);
    }
}
