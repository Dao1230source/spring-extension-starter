package org.source.spring.utility;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.source.utility.enums.BaseExceptionEnum;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;


@ConditionalOnProperty(prefix = "org.source.spring.enabled", name = "utility", matchIfMissing = true)
@AutoConfiguration
public class SystemUtil implements EnvironmentAware {
    private static String ip;
    @Getter
    private static int port;
    @Getter
    private static String applicationName;

    @Override
    public synchronized void setEnvironment(@NotNull Environment environment) {
        SystemUtil.applicationName = environment.getProperty("spring.application.name");
    }

    @EventListener
    public synchronized void onApplicationEvent(WebServerInitializedEvent webServerInitializedEvent) {
        try {
            SystemUtil.port = webServerInitializedEvent.getWebServer().getPort();
            InetAddress inetAddress = InetAddress.getLocalHost();
            SystemUtil.ip = inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            BaseExceptionEnum.GET_IP_PORT_FAIL.except(e);
        }
    }

    public static String getIp() {
        if (StringUtils.isBlank(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }

}
