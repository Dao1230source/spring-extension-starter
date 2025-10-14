package org.source.spring.i18n.validate;

import org.jetbrains.annotations.NotNull;
import org.source.spring.i18n.I18nConstant;
import org.source.spring.i18n.I18nWrapper;
import org.springframework.context.support.AbstractMessageSource;

import java.text.MessageFormat;
import java.util.Locale;

public class DictMessageSource extends AbstractMessageSource {
    @Override
    protected MessageFormat resolveCode(@NotNull String code, @NotNull Locale locale) {
        return I18nWrapper.findAsMessageFormat(locale, I18nConstant.GROUP_VALIDATE, code);
    }
}
