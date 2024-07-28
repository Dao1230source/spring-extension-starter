package org.source.spring.request;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;
import org.source.spring.scan.AbstractAnnotationScanProcessor;
import org.source.spring.scan.ScanConfig;
import org.source.spring.scan.ScanProcessor;
import org.source.utility.constant.Constants;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Strings;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.util.CollectionUtils;
import retrofit2.Retrofit;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Data
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "org.source.spring", name = "request", matchIfMissing = true)
@AutoConfigureBefore(ScanConfig.class)
@AutoConfiguration
public class RetrofitConfig implements BeanFactoryAware {
    private BeanFactory beanFactory;


    @Override
    public void setBeanFactory(@NotNull BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Bean
    public ScanProcessor requestScanProcessor(List<Interceptor> interceptorList,
                                              RequestProperties requestProperties) {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        if (!CollectionUtils.isEmpty(interceptorList)) {
            interceptorList.forEach(clientBuilder::addInterceptor);
        }
        OkHttpClient client = clientBuilder.build();
        return new AbstractAnnotationScanProcessor(List.of(Rest.class)) {

            @Override
            public void processClasses(@NotNull List<Class<?>> classes) {
                classes.stream().collect(Collectors.groupingBy(k -> k.getAnnotation(Rest.class).name())).forEach((k, v) -> {
                    log.debug("retrofit:{}", k);
                    RequestProperties.RetrofitProperties properties = requestProperties.getRetrofits().get(k);
                    if (Objects.isNull(properties)) {
                        throw new IllegalArgumentException(String.format("properties of name:%s is null", k));
                    }
                    AdviceJacksonConverterFactory converterFactory = new AdviceJacksonConverterFactory(
                            k, Jsons.getInstance(), properties.isAutoUnpackResponse(), properties.isAutoPackRequest());
                    Retrofit retrofit = new Retrofit.Builder().client(client).addCallAdapterFactory(new CallAdapterFactory())
                            .addConverterFactory(converterFactory).baseUrl(properties.getBaseUrl()).build();
                    v.forEach(cls -> {
                        String beanName = Strings.removePrefixAndLowerFirst(cls.getSimpleName(), Constants.EMPTY);
                        if (beanFactory instanceof BeanDefinitionRegistry registry) {
                            AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition().getBeanDefinition();
                            // interface 生成 bean 的 FactoryBean class
                            beanDefinition.setBeanClass(RestFactoryBean.class);
                            // 给 FactoryBean 对象的属性赋值
                            beanDefinition.getPropertyValues().add("cls", cls);
                            beanDefinition.getPropertyValues().add("retrofit", retrofit);
                            beanDefinition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_NAME);
                            registry.registerBeanDefinition(beanName, beanDefinition);
                        }
                    });
                });
            }
        };
    }

    @Data
    public static class RestFactoryBean implements FactoryBean<Object> {
        private Class<?> cls;
        private Retrofit retrofit;

        @Override
        public Object getObject() {
            return retrofit.create(cls);
        }

        @Override
        public Class<?> getObjectType() {
            return cls;
        }
    }
}