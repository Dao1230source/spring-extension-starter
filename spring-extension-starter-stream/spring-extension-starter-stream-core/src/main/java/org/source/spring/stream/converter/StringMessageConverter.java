package org.source.spring.stream.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.extern.slf4j.Slf4j;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.utils.Jsons;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.AbstractMessageConverter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
* String 消息转换器
* <pre>
* 支持 String/[String]格式的数据转换
* 如果发送端是string，接收端可以是 Object/[Object]
* spring cloud stream 处理消息时如果发送端和接收端都是list，stream会自动循环处理
* 如果发送端是[String]，接收端必须 [Object]
* </pre>
*/
@Order(1)
@Slf4j
public class StringMessageConverter extends AbstractMessageConverter {
    @Override
    protected boolean supports(Class<?> clazz) {
        return true;
    }

    @Nullable
    @Override
    protected Object convertFromInternal(Message<?> message, Class<?> targetClass, @Nullable Object conversionHint) {
        Object payload = message.getPayload();
        return convert((String) payload, targetClass, conversionHint);
    }

    private Object convert(String str, Class<?> targetClass, @Nullable Object conversionHint) {
        try (JsonParser jsonParser = Jsons.getInstance().createParser(str.trim())) {
            JsonToken firstToken = jsonParser.nextToken();
            if (firstToken == JsonToken.START_ARRAY) {
                return Jsons.list(str, targetClass);
            } else if (Collection.class.isAssignableFrom(targetClass)
                    && Objects.nonNull(conversionHint)
                    && conversionHint instanceof ParameterizedType parameterizedType) {
                Class<?>[] classes = Arrays.stream(parameterizedType.getActualTypeArguments()).map(Class.class::cast)
                        .toArray(Class<?>[]::new);
                Object obj = Jsons.obj(str, Jsons.getJavaType(classes));
                return List.of(obj);
            } else {
                return Jsons.obj(str, targetClass);
            }
        } catch (IOException e) {
            throw BaseExceptionEnum.JSON_STRING_2_OBJECT_EXCEPTION.except(e);
        }
    }

}