package org.source.spring.scan;

import org.source.utility.utils.Streams;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface ExtendPackagesProcessor {

    /**
     * 扩展包名
     *
     * @return packages
     */
    @NonNull
    List<String> extendPackages();

    /**
     * 用来分组的class
     *
     * @return config class
     */
    @NonNull
    Class<?> groupClass();

    /**
     * 扩展包扫描完并配置后，需要依赖上述过程生成的bean的config class，交给 after()执行
     * 注意，不要使用@Configuration等注解自动注册，必须手动调用 after 方法注册
     */
    @Nullable
    default void after(@NonNull BeanFactory beanFactory) {
    }

    /**
     * 获取包名，包括主程序的报名
     *
     * @param beanFactory beanFactory
     * @param configClass configClass
     * @return packages
     */
    static List<String> getPackagesWithApp(@NonNull BeanFactory beanFactory, @NonNull Class<?> configClass) {
        // 解析主程序的包名
        List<String> packages = new ArrayList<>(AutoConfigurationPackages.get(beanFactory));
        packages.addAll(getPackages(beanFactory, configClass));
        return packages;
    }

    /**
     * 获取包名
     *
     * @param beanFactory beanFactory
     * @param groupClass  groupClass
     * @return packages
     */
    static List<String> getPackages(@NonNull BeanFactory beanFactory, @NonNull Class<?> groupClass) {
        // 解析配置的额外的包名
        List<ExtendPackagesProcessor> extendPackagesProcessorList = extendPackagesProcessorList(beanFactory, groupClass);
        return Streams.of(extendPackagesProcessorList).map(ExtendPackagesProcessor::extendPackages).flatMap(Collection::stream).toList();
    }

    static List<ExtendPackagesProcessor> extendPackagesProcessorList(@NonNull BeanFactory beanFactory,
                                                                     @NonNull Class<?> groupClass) {
        return beanFactory.getBeanProvider(ExtendPackagesProcessor.class)
                .stream().filter(k -> groupClass.equals(k.groupClass())).toList();
    }
}
