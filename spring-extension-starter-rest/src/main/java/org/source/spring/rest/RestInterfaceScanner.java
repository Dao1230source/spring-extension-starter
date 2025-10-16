package org.source.spring.rest;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.source.spring.common.utility.ImportRegistrarUtil;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.utils.Jsons;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import retrofit2.Retrofit;

import java.util.Map;
import java.util.Set;

@Slf4j
public class RestInterfaceScanner extends ClassPathScanningCandidateComponentProvider {
    public static final String PROPERTIES_PREFIX_SECURITY = "org.source.security.";
    public static final String PROPERTIES_PREFIX_REST = "org.source.spring.rests.";
    private final BeanDefinitionRegistry registry;
    private final Environment environment;
    private final ResourceLoader resourceLoader;
    private final OkHttpClient okHttpClient;

    public RestInterfaceScanner(BeanDefinitionRegistry registry, Environment environment, ResourceLoader resourceLoader) {
        super(false);
        setResourceLoader(resourceLoader);
        this.registry = registry;
        this.environment = environment;
        this.resourceLoader = resourceLoader;
        this.okHttpClient = this.obtainOkHttpClient();
    }

    protected OkHttpClient obtainOkHttpClient() {
        String secretKeyName = PROPERTIES_PREFIX_SECURITY + "secret-key";
        String secretKey = environment.getProperty(secretKeyName, "");
        BaseExceptionEnum.NOT_EMPTY.notEmpty(secretKey, secretKeyName);
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .addInterceptor(new RestInterceptor(secretKey));
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
        RestProperties.NamedRestProperties properties = this.obtainRestProperties(restName);
        AdviceJacksonConverterFactory converterFactory = new AdviceJacksonConverterFactory(
                restName, Jsons.getInstance(), properties.isAutoUnpackResponse(), properties.isAutoPackRequest());
        Retrofit retrofit = new Retrofit.Builder().client(this.okHttpClient).addCallAdapterFactory(new CallAdapterFactory())
                .addConverterFactory(converterFactory).baseUrl(properties.getBaseUrl()).build();
        Class<?> restClass = ClassUtils.resolveClassName(restName, this.resourceLoader.getClassLoader());
        ImportRegistrarUtil.registerBeanDefinition(registry, restClass, () -> retrofit.create(restClass));
    }

    protected RestProperties.NamedRestProperties obtainRestProperties(String restName) {
        String baseUrl = environment.getProperty(PROPERTIES_PREFIX_REST + restName + ".baseUrl");
        if (!StringUtils.hasLength(baseUrl)) {
            throw new IllegalArgumentException(String.format("properties of name:%s is null", restName));
        }
        boolean autoUnpackResponse = Boolean.parseBoolean(environment.getProperty(PROPERTIES_PREFIX_REST + restName + ".autoUnpackResponse", "true"));
        boolean autoPackRequest = Boolean.parseBoolean(environment.getProperty(PROPERTIES_PREFIX_REST + restName + ".autoPackRequest", "false"));
        return new RestProperties.NamedRestProperties(baseUrl, autoUnpackResponse, autoPackRequest);
    }

}