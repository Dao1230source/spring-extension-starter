package org.source.spring.i18n;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.jetbrains.annotations.Nullable;
import org.source.spring.i18n.annotation.I18n;
import org.source.spring.i18n.enums.I18nRefTypeEnum;
import org.springframework.context.i18n.LocaleContextHolder;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Objects;

public class I18nSerializer extends JsonSerializer<String> {
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        String currentName = gen.getOutputContext().getCurrentName();
        Object currentValue = gen.getCurrentValue();
        Field field = getField(currentValue.getClass(), currentName);
        if (Objects.isNull(field)) {
            gen.writeString(value);
            return;
        }
        I18n i18n = field.getAnnotation(I18n.class);
        if (Objects.isNull(i18n)) {
            gen.writeString(value);
            return;
        }
        String group = i18n.group();
        if (!Void.class.equals(i18n.groupClass())) {
            group = i18n.groupClass().getName();
        }
        String key = I18nRefTypeEnum.getValue(i18n.key(), currentValue, value);
        assert key != null;
        String v = I18nWrapper.find(LocaleContextHolder.getLocale(), group, key);
        gen.writeString(v);
    }

    private @Nullable Field getField(Class<?> aClass, String fieldName) {
        Field field;
        Class<?> clz = aClass;
        do {
            try {
                field = clz.getDeclaredField(fieldName);
            } catch (Exception e) {
                field = null;
            }
            clz = clz.getSuperclass();
        } while (Objects.isNull(field) && Objects.nonNull(clz));
        return field;
    }
}
