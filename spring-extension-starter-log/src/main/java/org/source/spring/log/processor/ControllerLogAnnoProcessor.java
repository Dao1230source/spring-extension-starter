package org.source.spring.log.processor;

import org.source.spring.log.Logs;
import org.source.spring.log.annotation.LogContext;
import org.source.spring.log.enums.LogSystemTypeEnum;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;

public class ControllerLogAnnoProcessor extends AbstractLogAnnotationProcessor<LogContext, ControllerLogAnnoProcessor> {

    @Override
    public void before(MethodDetail<LogContext> detail) {
        Logs.putLogContext();
        Logs.setLogContextSystemType(LogSystemTypeEnum.WEB);
    }

    @Override
    public void finals(MethodDetail<LogContext> detail) {
        Logs.removeLogContext();
    }

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return targetClass.isAnnotationPresent(RestController.class) || targetClass.isAnnotationPresent(Controller.class);
    }

    @Override
    public int order() {
        return 1;
    }
}