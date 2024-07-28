package org.source.spring.i18n.facade.data;

import lombok.Data;
import org.source.spring.i18n.annotation.I18nRef;

@Data
public class EnumData {
    private Class<? extends Enum<?>> enumClass;
    private String group;
    private I18nRef key;
    private I18nRef value;
}
