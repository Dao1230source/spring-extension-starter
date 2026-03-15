package org.source.spring.io;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 系统输入数据包装，一般用于和其他系统交互，添加其他操作，比如加解密、验证等
 *
 * @param <T>
 */
@Data
public class Input<T> {

    @NotNull(message = "Input.data不能为空")
    @Valid
    private T data;

    public static <T> Input<T> of(T data) {
        Input<T> input = new Input<>();
        input.setData(data);
        return input;
    }
}
