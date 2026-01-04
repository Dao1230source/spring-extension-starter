package org.source.spring.rest;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.source.spring.common.io.Request;
import org.source.spring.common.io.Response;
import org.source.utility.utils.Jsons;
import retrofit2.Converter;
import retrofit2.Retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@Slf4j
public class AdviceJacksonConverterFactory extends Converter.Factory {
    private static final MediaType MEDIA_TYPE = MediaType.get("application/json; charset=UTF-8");
    private final String name;
    private final ObjectMapper mapper;
    private final boolean autoUnpackResponse;
    private final boolean autoPackRequest;

    public AdviceJacksonConverterFactory(String name,
                                         ObjectMapper mapper,
                                         boolean autoUnpackResponse,
                                         boolean autoPackRequest) {
        this.name = name;
        this.mapper = mapper;
        this.autoUnpackResponse = autoUnpackResponse;
        this.autoPackRequest = autoPackRequest;
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type,
                                                            Annotation @NotNull [] annotations,
                                                            Retrofit retrofit) {
        JavaType javaType = mapper.getTypeFactory().constructType(type);
        boolean autoUnpack = isAutoUnpackResponse(type);
        if (autoUnpack) {
            log.debug("auto unpack data from org.source.web.io.Response:{}", type.getTypeName());
            javaType = mapper.getTypeFactory().constructParametricType(Response.class, javaType);
        }
        ObjectReader reader = mapper.readerFor(javaType);
        return (Converter<ResponseBody, Object>) value -> {
            try (value) {
                Object responseBody = reader.readValue(value.charStream());
                if (log.isDebugEnabled()) {
                    log.debug("response body:{}", Jsons.str(responseBody));
                }
                if (autoUnpack && responseBody instanceof Response<?> response) {
                    return response.getData();
                }
                return responseBody;
            }
        };
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(Type type,
                                                          Annotation[] parameterAnnotations,
                                                          Annotation[] methodAnnotations,
                                                          Retrofit retrofit) {
        JavaType javaType = mapper.getTypeFactory().constructType(type);
        boolean packRequest = isAutoPackRequest(type);
        if (packRequest) {
            log.debug("name:{} auto pack data to org.source.web.io.Request:{}", this.name, type.getTypeName());
            javaType = mapper.getTypeFactory().constructParametricType(Request.class, javaType);
        }
        ObjectWriter writer = mapper.writerFor(javaType);
        return (Converter<Object, RequestBody>) value -> {
            Object param = value;
            if (packRequest) {
                param = Request.of(param);
            }
            if (log.isDebugEnabled()) {
                log.debug("request body:{}", Jsons.str(param));
            }
            byte[] bytes = writer.writeValueAsBytes(param);
            return RequestBody.create(bytes, MEDIA_TYPE);
        };
    }

    private boolean isAutoUnpackResponse(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            return autoUnpackResponse && !parameterizedType.getRawType().getTypeName().equals(Response.class.getName());
        }
        return autoUnpackResponse && !type.getTypeName().equals(Response.class.getName());
    }

    private boolean isAutoPackRequest(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            return autoPackRequest && !parameterizedType.getRawType().getTypeName().equals(Request.class.getName());
        }
        return autoPackRequest && !type.getTypeName().equals(Request.class.getName());
    }
}
