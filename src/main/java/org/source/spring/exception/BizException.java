package org.source.spring.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.source.utility.exceptions.BaseException;
import org.source.utility.exceptions.EnumProcessor;
import org.source.utility.utils.Strings;
import org.source.spring.i18n.I18nConstant;
import org.source.spring.i18n.annotation.I18n;
import org.source.spring.i18n.annotation.I18nRef;
import org.source.spring.i18n.enums.I18nRefTypeEnum;
import org.springframework.util.StringUtils;

/**
 * @author zengfugen
 */
@EqualsAndHashCode(callSuper = false)
@Getter
public class BizException extends BaseException {
    private final String code;
    @I18n(group = I18nConstant.GROUP_EXCEPTION, key = @I18nRef(type = I18nRefTypeEnum.FIELD, value = "code"))
    private final String message;
    private final Throwable cause;
    @I18n(group = I18nConstant.GROUP_EXCEPTION_MESSAGE, key = @I18nRef(type = I18nRefTypeEnum.SELF))
    private final String extraMessage;

    @Setter
    @Nullable
    private transient Object data;

    public BizException(@NotNull EnumProcessor<?> content,
                        @Nullable Throwable cause,
                        @Nullable String extraMessage,
                        @Nullable Object... objects) {
        super(content, cause, extraMessage, objects);
        this.code = content.getCode();
        this.message = content.getMessage();
        this.cause = cause;
        this.extraMessage = StringUtils.hasText(extraMessage) ? Strings.format(extraMessage, objects) : null;
    }

    public BizException(EnumProcessor<?> content, String extraMessage, Object... objects) {
        this(content, null, extraMessage, objects);
    }

    public BizException(EnumProcessor<?> content, Throwable e) {
        this(content, e, null);
    }

    public BizException(EnumProcessor<?> content) {
        this(content, null);
    }

    public BizException(BaseException exception) {
        super(exception.getCode(), exception.getMessage(), exception.getCause(), exception.getExtraMessage());
        this.code = exception.getCode();
        this.message = exception.getMessage();
        this.cause = exception.getCause();
        this.extraMessage = exception.getExtraMessage();
    }

}
