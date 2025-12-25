package org.source.spring.stream.rest;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.source.spring.stream.Listener;
import org.source.spring.stream.template.ConsumerProcessor;
import org.source.utility.enums.BaseExceptionEnum;
import org.springframework.cloud.stream.provisioning.ProvisioningException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@AllArgsConstructor
@Data
public class RestConsumerProcessor implements ConsumerProcessor {
    public static final String CONTEXT_PATH = "stream";
    private RequestMappingHandlerMapping handlerMapping;
    private final String path;

    @Override
    public Listener createConsumer() throws ProvisioningException {
        return mapping();
    }

    private Listener mapping() {
        Path path1 = Paths.get(CONTEXT_PATH, this.path);
        // 创建 RequestMappingInfo
        RequestMappingInfo mappingInfo = RequestMappingInfo
                .paths(path1.toString())
                .methods(RequestMethod.POST)
                .headers("Content-Type=application/json")
                .consumes(MediaType.APPLICATION_JSON_VALUE)
                .produces(MediaType.APPLICATION_JSON_VALUE)
                .build();
        // 创建 HandlerMethod
        Listener restListener = new RestListener();
        Method method;
        try {
            method = RestListener.class.getMethod("handleRequest", HttpServletRequest.class);
        } catch (NoSuchMethodException e) {
            throw BaseExceptionEnum.RUNTIME_EXCEPTION.except(e);
        }
        HandlerMethod handlerMethod = new HandlerMethod(restListener, method);
        // 注册映射
        handlerMapping.registerMapping(mappingInfo, restListener, handlerMethod.getMethod());
        return restListener;
    }
}