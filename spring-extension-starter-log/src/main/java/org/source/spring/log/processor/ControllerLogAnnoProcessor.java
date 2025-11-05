package org.source.spring.log.processor;

import org.source.spring.log.Logs;
import org.source.spring.log.annotation.LogContext;
import org.source.spring.log.enums.LogScopeEnum;
import org.source.spring.log.enums.LogSystemTypeEnum;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;

public class ControllerLogAnnoProcessor extends AbstractLogAnnotationProcessor<LogContext> {

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return targetClass.isAnnotationPresent(RestController.class) || targetClass.isAnnotationPresent(Controller.class);
    }

    @Override
    public LogScopeEnum getLogScope() {
        return LogScopeEnum.LOG_CONTEXT;
    }

    @Override
    public void before(MethodDetail<LogContext> detail) {
        Logs.setLogContextSystemType(LogSystemTypeEnum.WEB);
    }

    @Override
    public int order() {
        return 1;
    }
}