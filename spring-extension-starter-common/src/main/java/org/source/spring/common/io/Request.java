package org.source.spring.common.io;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Request<T> {

    @NotNull(message = "Request.data不能为空")
    @Valid
    private T data;

    public static <T> Request<T> of(T data) {
        Request<T> request = new Request<>();
        request.setData(data);
        return request;
    }
}
