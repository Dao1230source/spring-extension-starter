package org.source.spring.common.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.source.utility.exceptions.BaseException;
import org.source.utility.exceptions.EnumProcessor;
import org.source.utility.utils.Strings;
import org.springframework.util.StringUtils;

/**
 * spring extension exception
 */
@EqualsAndHashCode(callSuper = false)
@Getter
public class SpExtException extends BaseException {
    private final String code;
    private final String message;
    private final Throwable cause;
    private final String extraMessage;

    @Setter
    @Nullable
    private transient Object data;

    public SpExtException(@NotNull EnumProcessor<?> content,
                          @Nullable Throwable cause,
                          @Nullable String extraMessage,
                          @Nullable Object... objects) {
        super(content, cause, extraMessage, objects);
        this.code = content.getCode();
        this.message = content.getMessage();
        this.cause = cause;
        this.extraMessage = StringUtils.hasText(extraMessage) ? Strings.format(extraMessage, objects) : null;
    }

    public SpExtException(EnumProcessor<?> content, String extraMessage, Object... objects) {
        this(content, null, extraMessage, objects);
    }

    public SpExtException(EnumProcessor<?> content, Throwable e) {
        this(content, e, null);
    }

    public SpExtException(EnumProcessor<?> content) {
        this(content, null);
    }

    public SpExtException(BaseException exception) {
        super(exception.getCode(), exception.getMessage(), exception.getCause(), exception.getExtraMessage());
        this.code = exception.getCode();
        this.message = exception.getMessage();
        this.cause = exception.getCause();
        this.extraMessage = exception.getExtraMessage();
    }

}
