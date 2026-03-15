package org.source.spring.io;

import com.fasterxml.jackson.annotation.JsonGetter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.source.spring.trace.TraceContext;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.exception.BaseException;
import org.source.utility.exception.EnumProcessor;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Function;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Output<T> {
    public static final String CODE_SUCCESS = "SUCCESS";
    public static final String SUCCESS_MESSAGE = "执行成功";
    public static final String CODE_FAILED = "FAILED";

    private String code;

    private String message;
    private String extraMessage;
    private Throwable cause;

    private LocalDateTime timestamp;
    private String traceId;

    private T data;

    public boolean isSuccess() {
        return CODE_SUCCESS.equals(code);
    }

    public <E> Output<E> map(Function<T, E> converter) {
        if (Objects.nonNull(this.data)) {
            return Output.success(converter.apply(this.data));
        }
        return Output.success();
    }

    public static <T> Output<T> success() {
        return Output.<T>builder().code(CODE_SUCCESS).message(SUCCESS_MESSAGE)
                .traceId(TraceContext.getTraceId()).timestamp(LocalDateTime.now()).build();
    }

    public static <T> Output<T> success(T content) {
        return Output.<T>builder().code(CODE_SUCCESS).message(SUCCESS_MESSAGE).data(content)
                .traceId(TraceContext.getTraceId()).timestamp(LocalDateTime.now()).build();
    }

    public static <T> Output<T> fail(EnumProcessor<?> e) {
        return Output.<T>builder().code(e.getCode()).message(e.getMessage())
                .traceId(TraceContext.getTraceId()).timestamp(LocalDateTime.now()).build();
    }

    public static <T> Output<T> fail(EnumProcessor<?> e, String extraMessage) {
        return Output.<T>builder().code(e.getCode()).message(extraMessage)
                .traceId(TraceContext.getTraceId()).timestamp(LocalDateTime.now()).build();
    }

    public static <T> Output<T> fail(String code, String extraMessage) {
        return Output.<T>builder().code(code).message(extraMessage)
                .traceId(TraceContext.getTraceId()).timestamp(LocalDateTime.now()).build();
    }

    public static <T> Output<T> fail(Throwable throwable) {
        log.error(ExceptionUtils.getStackTrace(throwable));
        if (throwable instanceof BaseException baseException) {
            return Output.<T>builder().code(baseException.getCode()).message(baseException.getMessage())
                    .extraMessage(baseException.getExtraMessage()).cause(baseException.getCause())
                    .traceId(TraceContext.getTraceId()).timestamp(LocalDateTime.now()).build();
        }
        return Output.<T>builder().code(CODE_FAILED).message(throwable.getMessage())
                .traceId(TraceContext.getTraceId()).timestamp(LocalDateTime.now()).build();
    }

    public static <T> T getOrThrow(Output<T> output) {
        BaseExceptionEnum.NOT_NULL.nonNull(output, "response is null");
        BaseExceptionEnum.NOT_NULL.nonNull(output.getData(), "response's data is null");
        return output.getData();
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
