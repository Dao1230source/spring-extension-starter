package org.source.spring.mq.config;

import com.fasterxml.jackson.databind.JavaType;
import lombok.extern.slf4j.Slf4j;
import org.source.utility.utils.Jsons;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.AbstractMessageConverter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

@Slf4j
@ConditionalOnProperty(prefix = "org.source.spring.enabled", value = "mq", matchIfMissing = true)
@AutoConfigureBefore
public class MqMessageConverter extends AbstractMessageConverter {
    @Override
    protected boolean supports(@NonNull Class<?> clazz) {
        return true;
    }

    @Override
    protected Object convertFromInternal(@NonNull Message<?> message, @NonNull Class<?> targetClass, Object conversionHint) {
        // sf-kafka只支持批量消费
        Object obj = null;
        try {
            byte[] bytes;
            // kafka 生产者的message payload必是json string，此时targetClass是 Object
            Object payload = message.getPayload();
            if (payload instanceof String str) {
                bytes = str.getBytes();
            }
            // 但如果是通过stream直发consumer的是 byte[],此时targetClass是 List<Object>
            else if (payload instanceof byte[] bs) {
                bytes = bs;
            }
            // 但有时候stream会对消费过程进行优化，producer不经过sfKafka直达consumer，此时是原始的数据类型
            else {
                if (payload instanceof Collection) {
                    return new ArrayList<>(((Collection<?>) payload));
                } else {
                    return Collections.singletonList(payload);
                }
            }
            obj = convert(bytes, targetClass, conversionHint);
        } catch (Exception e) {
            log.error("sf-kafka convert message to object exception", e);
        }
        return obj;
    }

    private static Object convert(byte[] bytes, @NonNull Class<?> targetClass, Object conversionHint) {
        // 如果是List
        JavaType javaType;
        if (List.class.isAssignableFrom(targetClass) && conversionHint instanceof ParameterizedType parameterizedType) {
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (Objects.isNull(actualTypeArguments) || actualTypeArguments.length == 0) {
                javaType = Jsons.getJavaType(parameterizedType.getRawType());
            } else {
                Class<?>[] classes = Arrays.stream(actualTypeArguments).map(Class.class::cast).toArray(Class<?>[]::new);
                javaType = Jsons.getJavaType(classes);
            }
            Object obj = Jsons.obj(Arrays.toString(bytes), javaType);
            return obj instanceof List ? obj : Collections.singletonList(obj);
        } else {
            javaType = Jsons.getJavaType(targetClass);
            return Jsons.obj(bytes, javaType);
        }
    }

}
