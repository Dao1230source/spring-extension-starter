package org.source.spring.stream.properties;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
public class Router {
    /**
     * 路由目标
     */
    @NotEmpty(message = "目标生产者(producerName)名称不能为空")
    private String producerName;
    /**
     * 过滤条件，spEl表达式。如果为空，全部转发
     */
    private String condition;
}
