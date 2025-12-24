package org.source.spring.stream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.util.Assert;

@Slf4j
public record StreamMessageHandler<P>(ProducerDestination destination,
                                      ExtendedProducerProperties<P> producerProperties,
                                      MessageChannel errorChannel) implements MessageHandler {

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        Assert.notNull(this.destination, "ProducerDestination must not be null");
        Assert.isTrue(this.destination instanceof StreamProducerDestination, "destination must instanceof StreamProducerDestination");
        StreamProducerDestination streamProducerDestination = (StreamProducerDestination) destination;
        Assert.hasText(streamProducerDestination.getName(), "ProducerDestination name must not be empty");
        /*
         * 接收stream输出通道的消息
         * 转发到消息中间件的生产者
         */
        Producer producer = streamProducerDestination.producer();
        Assert.notNull(producer, String.format("There is no corresponding producer client for name:%s", this.destination.getName()));
        try {
            log.debug("producer send message:{}", message);
            producer.send(message);
        } catch (Exception e) {
            log.error("send message to kafka error", e);
            if (this.errorChannel != null) {
                this.errorChannel.send(new ErrorMessage(new MessagingException(message, "Failed to send message to " + this.destination.getName(), e)));
            }
        }
    }
}