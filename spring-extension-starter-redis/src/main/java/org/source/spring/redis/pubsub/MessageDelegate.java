package org.source.spring.redis.pubsub;
/**
 * 处理 redis SUBSCRIBE 消息
 * @author zengfugen
 */
public interface MessageDelegate {
    /**
     * 默认的监听处理消息的方法
     */
    String DEFAULT_LISTENER_METHOD = "handleMessage";

    /**
     * 监听消息的方法
     *
     * @return 方法名
     */
    default String listenerMethod() {
        return DEFAULT_LISTENER_METHOD;
    }

    /**
     * 通道主题
     *
     * @return 通道
     */
    String channelTopic();

    /**
     * 处理消息的方法
     *
     * @param message 消息
     */
    void handleMessage(String message);
}
