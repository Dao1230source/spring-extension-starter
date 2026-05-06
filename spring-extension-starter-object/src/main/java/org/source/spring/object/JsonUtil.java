package org.source.spring.object;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.utils.Jsons;

/**
 * 全局json配置在{@link Jsons}，默认属性为null或空时不序列化。
 * <p>
 * 这样会导致object body data 比较时会不想等，单独配置json工具
 */
@UtilityClass
public class JsonUtil {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.ALWAYS);
    }

    public static String str(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw BaseExceptionEnum.JSON_OBJECT_2_STRING_EXCEPTION.newException(e);
        }
    }

    public static <T> T obj(String jsonStr, Class<T> tClass) {
        try {
            return OBJECT_MAPPER.readValue(jsonStr, tClass);
        } catch (JsonProcessingException e) {
            throw BaseExceptionEnum.JSON_STRING_2_OBJECT_EXCEPTION.newException(e);
        }
    }

}
