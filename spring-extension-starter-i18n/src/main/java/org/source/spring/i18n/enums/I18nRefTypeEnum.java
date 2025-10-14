package org.source.spring.i18n.enums;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.source.spring.i18n.annotation.I18nRef;
import org.source.spring.i18n.annotation.I18nServer;
import org.source.utility.constant.Constants;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.utils.Reflects;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.Objects;

@Slf4j
public enum I18nRefTypeEnum {
    /**
     * class.getSimpleName()
     */
    CLASS {
        @Override
        public String getNameFromEnum(String value, Class<? extends Enum<?>> enumClass) {
            return enumClass.getSimpleName();
        }
    },
    /**
     * enum.name(),field.getName()
     */
    NAME {
        @Override
        public String getValue(String value, Object obj, @Nullable String source) {
            if (obj instanceof Enum<?> aEnum) {
                return aEnum.name();
            } else {
                throw BaseExceptionEnum.MUST_BE_ENUM_TYPE.except("type:{},obj class:{} not a Enum", this.name(), obj.getClass());
            }
        }
    },
    /**
     * field
     */
    FIELD {
        @Override
        public String getNameFromEnum(String value, Class<? extends Enum<?>> enumClass) {
            Field field = Reflects.getFieldByName(enumClass, value);
            if (Objects.nonNull(field)) {
                return field.getName();
            } else {
                throw BaseExceptionEnum.FIELD_NAME_INVALID.except("type:{}, field:{}.{} not exists", this.name(), enumClass.getName(), value);
            }
        }

        @Override
        public String getValue(String value, Object obj, @Nullable String source) {
            return String.valueOf(Reflects.getFieldValue(obj, value));
        }
    },
    /**
     * 字段本身的值
     */
    SELF {
        @Override
        public String getValue(String value, Object obj, @Nullable String source) {
            return source;
        }
    },
    /**
     * 直接量
     */
    LITERAL {
        @Override
        public String getNameFromEnum(String value, Class<? extends Enum<?>> enumClass) {
            return value;
        }

        @Override
        public String getValue(String value, Object obj, @Nullable String source) {
            return value;
        }
    };

    public @Nullable String getNameFromEnum(String value, Class<? extends Enum<?>> enumClass) {
        if (log.isDebugEnabled()) {
            log.debug("value:{}, enumClass:{}, name of {} default null", value, enumClass, this.name());
        }
        return null;
    }

    public @Nullable String getValue(String value, Object obj, @Nullable String source) {
        if (log.isDebugEnabled()) {
            log.debug("value:{}, obj:{}, source:{}, value of {} default null", value, obj, source, this.name());
        }
        return null;
    }

    public static @Nullable String getName(I18nRef i18nRef, Class<? extends Enum<?>> enumClass) {
        return i18nRef.type().getNameFromEnum(i18nRef.value(), enumClass);
    }

    public static @Nullable String getI18nDictGroup(I18nServer i18NServer, Class<? extends Enum<?>> enumClass, boolean single) {
        String groupName = getName(i18NServer.group(), enumClass);
        if (single) {
            return groupName;
        }
        String keyName = getName(i18NServer.key(), enumClass);
        if (StringUtils.hasText(keyName)) {
            groupName += Constants.COLON + keyName;

        }
        String valueName = getName(i18NServer.value(), enumClass);
        if (StringUtils.hasText(valueName)) {
            groupName += Constants.COLON + valueName;
        }
        return groupName;
    }


    public static @Nullable String getValue(I18nRef i18nRef, Enum<?> aEnum) {
        return getValue(i18nRef, aEnum, null);
    }

    public static @Nullable String getValue(I18nRef i18nRef, Object obj, @Nullable String source) {
        return i18nRef.type().getValue(i18nRef.value(), obj, source);
    }
}
