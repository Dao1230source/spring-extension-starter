package org.source.spring.stream.properties;

import org.springframework.lang.Nullable;

import java.util.List;

public interface ConsumerProperty extends Property {

    /**
     * 消费的消息可自动转发到目标通道
     * 注意：如果配置自动路由不可手动编写消费channel，否则自动路由会被覆盖不起作用。
     *
     * @return routers
     */
    @Nullable
    default List<Router> getRouters() {
        return null;
    }
}
