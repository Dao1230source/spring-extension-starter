package org.source.spring.log.handler;

import org.source.spring.log.LogAnnotationHandler;
import org.source.spring.log.Logs;
import org.source.spring.log.annotation.LogContext;
import org.source.spring.log.enums.LogSystemTypeEnum;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;

@AutoConfiguration
public class ControllerScopeHandler extends LogAnnotationHandler<LogContext, ControllerScopeHandler> {

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
    public ControllerScopeHandler getProcessor() {
        return this;
    }

    @Override
    protected int order() {
        return 1;
    }
}
