package org.source.spring.stream.converter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.extern.slf4j.Slf4j;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.utils.Jsons;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * json格式 String 消息转换器
 * <pre>
 * 一、推送消息全部转为json字符串
 * 二、接收消息全部转为对象
 * 说明：
 *     String 指json字符串，可以是对象转换的json串，也可以是对象数组转换的json串
 *     [String] 指 java String集合，比如{@code List<String>}
 *     同理，Object 指 java对象，比如{@code TestData}，
 *     [Object] 指 java对象集合，比如{@code List<TestData>}
 * 发送端支持 String/[String]格式的数据
 * 如果发送端是string，接收端可以是 Object/[Object]
 * spring cloud stream 处理消息时如果发送端和接收端都是list，stream会自动循环处理
 * 如果发送端是[String]，接收端必须 [Object]
 * </pre>
 */
@Slf4j
@AutoConfiguration
public class StringMessageConverter extends AbstractMessageConverter {
    @Override
    protected boolean supports(Class<?> clazz) {
        return true;
    }

    @Override
    protected @Nullable Object convertToInternal(Object payload, @Nullable MessageHeaders headers, @Nullable Object conversionHint) {
        return Jsons.str(payload);
    }

    @Nullable
    @Override
    protected Object convertFromInternal(Message<?> message, Class<?> targetClass, @Nullable Object conversionHint) {
        Object payload = message.getPayload();
        return convertFrom((String) payload, targetClass, conversionHint);
    }

    protected Object convertFrom(String str, Class<?> targetClass, @Nullable Object conversionHint) {
        try (JsonParser jsonParser = Jsons.getInstance().createParser(str.trim())) {
            boolean isArray = Collection.class.isAssignableFrom(targetClass)
                    && Objects.nonNull(conversionHint)
                    && conversionHint instanceof ParameterizedType;
            JsonToken firstToken = jsonParser.nextToken();
            if (isArray) {
                if (firstToken == JsonToken.START_ARRAY) {
                    return this.convertArrayStrToArray(str, (ParameterizedType) conversionHint);
                } else {
                    return this.convertStrToArray(str, (ParameterizedType) conversionHint);
                }
            } else {
                if (firstToken == JsonToken.START_ARRAY) {
                    return this.convertArrayStrToObject(str, targetClass);
                } else {
                    return this.convertStrToObject(str, targetClass);
                }
            }
        } catch (IOException e) {
            throw BaseExceptionEnum.JSON_STRING_2_OBJECT_EXCEPTION.except(e);
        }
    }

    protected Object convertArrayStrToArray(String arrayStr, ParameterizedType parameterizedType) {
        return Jsons.obj(arrayStr, Jsons.getJavaType(parameterizedType));
    }

    protected Object convertStrToArray(String str, ParameterizedType parameterizedType) {
        Class<?>[] classes = Arrays.stream(parameterizedType.getActualTypeArguments()).map(Class.class::cast).toArray(Class<?>[]::new);
        Object obj = Jsons.obj(str, Jsons.getJavaType(classes));
        return List.of(obj);
    }

    protected Object convertArrayStrToObject(String str, Class<?> targetClass) {
        return Jsons.list(str, targetClass);
    }

    protected Object convertStrToObject(String str, Class<?> targetClass) {
        return Jsons.obj(str, targetClass);
    }

}