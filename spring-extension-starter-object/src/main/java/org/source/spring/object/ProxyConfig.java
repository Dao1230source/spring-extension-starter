package org.source.spring.object;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@AutoConfiguration
@EnableAspectJAutoProxy(exposeProxy = true)
public class ProxyConfig {
}
