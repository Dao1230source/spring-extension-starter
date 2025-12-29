package org.source.spring.stream;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.source.spring.common.exception.SpExtException;
import org.source.utility.exceptions.EnumProcessor;

@Getter
@AllArgsConstructor
public enum StreamExceptionEnum implements EnumProcessor<SpExtException> {
    /**
     * stream
     */
    STREAM_PRODUCER_PROCESSOR_NOT_FOUND("stream producer processor not found"),
    STREAM_TARGET_CLASS_MUST_BE_OBJECT("目标对象必须是一个对象类型"),
    ;

    private final String message;

    @Override
    public String getCode() {
        return this.name();
    }
}