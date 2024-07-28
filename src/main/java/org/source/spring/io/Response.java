package org.source.spring.io;

import com.fasterxml.jackson.annotation.JsonGetter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.exceptions.BaseException;
import org.source.utility.exceptions.EnumProcessor;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Function;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Response<T> {
    public static final String CODE_SUCCESS = "SUCCESS";
    public static final String SUCCESS_MESSAGE = "执行成功";
    public static final String CODE_FAILED = "FAILED";

    private String code;

    private String message;
    private String extraMessage;
    private Throwable cause;

    private LocalDateTime timestamp;

    private T data;

    public boolean isSuccess() {
        return CODE_SUCCESS.equals(code);
    }

    public <E> Response<E> map(Function<T, E> converter) {
        if (Objects.nonNull(this.data)) {
            return Response.success(converter.apply(this.data));
        }
        return Response.success();
    }

    public static <T> Response<T> success() {
        return Response.<T>builder().code(CODE_SUCCESS)
                .message(SUCCESS_MESSAGE)
                .timestamp(LocalDateTime.now()).build();
    }

    public static <T> Response<T> success(T content) {
        return Response.<T>builder().code(CODE_SUCCESS)
                .message(SUCCESS_MESSAGE).data(content)
                .timestamp(LocalDateTime.now()).build();
    }

    public static <T> Response<T> fail(EnumProcessor<?> e) {
        return Response.<T>builder().code(e.getCode()).message(e.getMessage())
                .timestamp(LocalDateTime.now()).build();
    }

    public static <T> Response<T> fail(EnumProcessor<?> e, String extraMessage) {
        return Response.<T>builder().code(e.getCode()).message(extraMessage)
                .timestamp(LocalDateTime.now()).build();
    }

    public static <T> Response<T> fail(String code, String extraMessage) {
        return Response.<T>builder().code(code).message(extraMessage)
                .timestamp(LocalDateTime.now()).build();
    }

    public static <T> Response<T> fail(Throwable throwable) {
        log.error(ExceptionUtils.getStackTrace(throwable));
        if (throwable instanceof BaseException baseException) {
            return Response.<T>builder().code(baseException.getCode())
                    .message(baseException.getMessage()).extraMessage(baseException.getExtraMessage())
                    .cause(baseException.getCause())
                    .timestamp(LocalDateTime.now()).build();
        }
        return Response.<T>builder().code(CODE_FAILED).message(throwable.getMessage())
                .timestamp(LocalDateTime.now()).build();
    }

    public static <T> T getOrThrow(Response<T> response) {
        BaseExceptionEnum.NOT_NULL.nonNull(response, "response is null");
        BaseExceptionEnum.NOT_NULL.nonNull(response.getData(), "response's data is null");
        return response.getData();
    }

    @JsonGetter("cause")
    public String causeToString() {
        if (null == cause) {
            return null;
        }
        if (cause instanceof BaseException) {
            return cause.toString();
        } else {
            return cause.getMessage();
        }
    }

}
