package org.source.spring.redis.pubsub;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * springBoot 使用redis pub/sub功能
 * <pre>
 * PUBLISH:
 * {@code template.convertAndSend("message", "channelName"); }
 * </pre>
 *
 * @author zengfugen
 */
@AutoConfiguration
public class RedisPubsubConfig implements BeanFactoryAware {

    private DefaultListableBeanFactory beanFactory;

    /**
     * 作为组件依赖时，@AutoConfiguration执行时ApplicationContextWare还未执行，此时ApplicationContext=null
     *
     * @param beanFactory owning BeanFactory (never {@code null}).
     *                    The bean can immediately call methods on the factory.
     */
    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        Assert.isTrue(beanFactory instanceof DefaultListableBeanFactory,
                "spring boot beanFactory may be default as DefaultListableBeanFactory");
        this.beanFactory = (DefaultListableBeanFactory) beanFactory;
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        Map<String, MessageDelegate> messageDelegateMap = beanFactory.getBeansOfType(MessageDelegate.class);
        // 添加消息监听器
        messageDelegateMap.values().forEach(k -> {
            MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(k, k.listenerMethod());
            // 如果使用@Bean/@Component方式实现MessageListenerAdapter会自动执行该方法，这里主动调用
            listenerAdapter.afterPropertiesSet();
            container.addMessageListener(listenerAdapter, ChannelTopic.of(k.channelTopic()));
        });
        return container;
    }
}
