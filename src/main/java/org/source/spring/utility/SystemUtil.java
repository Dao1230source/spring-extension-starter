package org.source.spring.utility;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.source.utility.enums.BaseExceptionEnum;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;

import java.net.InetAddress;
import java.net.UnknownHostException;

@ConditionalOnProperty(prefix = "org.source.spring.enabled", name = "utility", matchIfMissing = true)
@AutoConfiguration
public class SystemUtil {
    private static String ip;
    @Getter
    private static int port;

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
