package org.source.spring.rest;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import org.source.spring.common.SecurityProperties;
import org.source.utility.constant.Constants;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Reflects;
import org.source.utility.utils.Strings;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import retrofit2.Retrofit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class RestInterfaceScanner extends ClassPathScanningCandidateComponentProvider {
    private final BeanDefinitionRegistry registry;
    private final ApplicationContext applicationContext;
    private final SecurityProperties securityProperties;
    private final RestProperties restProperties;
    private final OkHttpClient okHttpClient;

    public RestInterfaceScanner(BeanDefinitionRegistry registry, ApplicationContext applicationContext) {
        super(false);
        setResourceLoader((registry instanceof ResourceLoader resourceLoader ? resourceLoader : null));
        this.registry = registry;
        this.applicationContext = applicationContext;
        this.securityProperties = applicationContext.getBean(SecurityProperties.class);
        this.restProperties = applicationContext.getBean(RestProperties.class);
        this.okHttpClient = this.obtainOkHttpClient();
    }

    protected OkHttpClient obtainOkHttpClient() {
        List<Interceptor> restInterceptors = new ArrayList<>(this.applicationContext.getBeansOfType(Interceptor.class).values());
        restInterceptors.add(new RestInterceptor(securityProperties.getSecretKey()));
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        restInterceptors.forEach(clientBuilder::addInterceptor);
        return clientBuilder.build();
    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        // 确保只处理接口类型
        return beanDefinition.getMetadata().isInterface();
    }

    public void scan(String... basePackages) {
        for (String basePackage : basePackages) {
            // 查找候选组件
            Set<BeanDefinition> candidates = findCandidateComponents(basePackage);
            for (BeanDefinition candidate : candidates) {
                // 注册接口的Bean定义
                registerInterfaceBeanDefinition(candidate);
            }
        }
    }

    protected void registerInterfaceBeanDefinition(BeanDefinition beanDefinition) {
        // 实际注册逻辑
        String className = beanDefinition.getBeanClassName();
        log.debug("Registering interface: {}", className);
        AnnotationMetadata annotationMetadata = ((AnnotatedBeanDefinition) beanDefinition).getMetadata();
        Map<String, Object> annotationAttributes = annotationMetadata.getAnnotationAttributes(Rest.class.getName());
        assert annotationAttributes != null;
        String restName = (String) annotationAttributes.get("name");
        RestProperties.NamedRestProperties properties = this.restProperties.getRests().get(restName);
        AdviceJacksonConverterFactory converterFactory = new AdviceJacksonConverterFactory(
                restName, Jsons.getInstance(), properties.isAutoUnpackResponse(), properties.isAutoPackRequest());
        Retrofit retrofit = new Retrofit.Builder().client(this.okHttpClient).addCallAdapterFactory(new CallAdapterFactory())
                .addConverterFactory(converterFactory).baseUrl(properties.getBaseUrl()).build();
        Class<?> restClass = Reflects.classForNameOrDefault(beanDefinition.getBeanClassName(), null);
        GenericBeanDefinition restBeanDefinition = new GenericBeanDefinition();
        restBeanDefinition.setInstanceSupplier(() -> retrofit.create(restClass));
        String beanName = Strings.removePrefixAndLowerFirst(restClass.getSimpleName(), Constants.EMPTY);
        this.registry.registerBeanDefinition(beanName, restBeanDefinition);
    }

}