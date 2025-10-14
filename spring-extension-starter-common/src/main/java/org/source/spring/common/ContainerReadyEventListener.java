package org.source.spring.common;

import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Objects;
import java.util.function.Consumer;

@AllArgsConstructor
public class ContainerReadyEventListener implements ApplicationListener<ContextRefreshedEvent> {
    private final Consumer<ContextRefreshedEvent> consumer;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 确保是根应用上下文的事件，避免重复触发（例如在Web应用中）
        if (Objects.isNull(consumer) || Objects.nonNull(event.getApplicationContext().getParent())) {
            return;
        }
        // 在这里编写容器完全刷新后需要执行的代码
        // ApplicationContext 已经准备就绪，可以安全地获取任何Bean
        consumer.accept(event);
    }
}