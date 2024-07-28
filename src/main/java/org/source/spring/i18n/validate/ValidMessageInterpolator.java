package org.source.spring.i18n.validate;

import org.hibernate.validator.messageinterpolation.AbstractMessageInterpolator;
import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;
import org.source.spring.i18n.annotation.I18nIgnore;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.ResourceBundle;

public class ValidMessageInterpolator extends AbstractMessageInterpolator {
    private final ResourceBundleLocator resourceBundleLocator;

    public ValidMessageInterpolator(ResourceBundleLocator userResourceBundleLocator) {
        super(userResourceBundleLocator);
        this.resourceBundleLocator = userResourceBundleLocator;
    }

    @Override
    protected String interpolate(Context context, Locale locale, String term) {
        return term;
    }

    @Override
    public String interpolate(String message, Context context, Locale locale) {
        String i18nMessage = null;
        if (!context.getValidatedValue().getClass().isAnnotationPresent(I18nIgnore.class)) {
            ResourceBundle resourceBundle = resourceBundleLocator.getResourceBundle(locale);
            i18nMessage = resourceBundle.getString(message);
        }
        if (StringUtils.hasText(i18nMessage)) {
            message = i18nMessage;
        }
        return super.interpolate(message, context, locale);
    }
}
