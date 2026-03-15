package org.source.spring.i18n.validate;

import org.source.spring.i18n.I18nConstant;
import org.source.spring.i18n.I18nWrapper;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.lang.NonNull;

import java.text.MessageFormat;
import java.util.Locale;

public class DictMessageSource extends AbstractMessageSource {
    @Override
    protected MessageFormat resolveCode(@NonNull String code, @NonNull Locale locale) {
        return I18nWrapper.findAsMessageFormat(locale, I18nConstant.GROUP_VALIDATE, code);
    }
}
