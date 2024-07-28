package org.source.spring.io;

import jakarta.validation.Valid;
import lombok.Data;

@Data
public class Request<T> {

    @Valid
    private T data;

    public static <T> Request<T> of(T data) {
        Request<T> request = new Request<>();
        request.setData(data);
        return request;
    }
}
