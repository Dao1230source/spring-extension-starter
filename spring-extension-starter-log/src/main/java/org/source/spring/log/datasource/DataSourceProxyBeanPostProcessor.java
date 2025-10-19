package org.source.spring.log.datasource;

import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.listener.logging.CommonsLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class DataSourceProxyBeanPostProcessor implements BeanPostProcessor, Ordered {
    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 20;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource actualDataSource && !(bean instanceof ProxyDataSource)) {
            final ProxyFactory proxyFactory = new ProxyFactory(actualDataSource);
            proxyFactory.addAdvice(new ProxyDataSourceInterceptor(actualDataSource));
            proxyFactory.setProxyTargetClass(true);
            return proxyFactory.getProxy();
        }
        return bean;
    }

    private record ProxyDataSourceInterceptor(DataSource dataSource) implements MethodInterceptor {
        private ProxyDataSourceInterceptor(final DataSource dataSource) {
            this.dataSource = ProxyDataSourceBuilder.create(dataSource)
                    .logQueryByCommons(CommonsLogLevel.INFO)
                    .countQuery()
                    .logSlowQueryByCommons(1, TimeUnit.MINUTES)
                    .proxyResultSet()
                    .asJson()
                    .listener(new DataSourceChangeListener())
                    .build();
        }

        @Override
        public @Nullable Object invoke(final MethodInvocation invocation) throws Throwable {
            final Method proxyMethod = ReflectionUtils.findMethod(this.dataSource.getClass(),
                    invocation.getMethod().getName());
            if (proxyMethod != null) {
                return proxyMethod.invoke(this.dataSource, invocation.getArguments());
            }
            return invocation.proceed();
        }
    }

}
