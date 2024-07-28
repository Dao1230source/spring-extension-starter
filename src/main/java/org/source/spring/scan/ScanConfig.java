package org.source.spring.scan;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 本模块旨在扫描指定包下的所有class，获取符合条件的class并执行一定的逻辑
 * 1、如果需要增加额外的包名，配置{@link ExtendPackagesProcessor}的bean
 * 2、如果需要增加处理器，配置{@link ScanProcessor}的bean
 * 3、以上1、2的配置类均需要使用 @AutoConfigureBefore(ScanConfig.class) 使其先于ScanConfig加载
 */
@Data
@AutoConfiguration
public class ScanConfig implements BeanFactoryAware, InitializingBean {
    private BeanFactory beanFactory;
    private boolean loaded = false;

    @Override
    public void setBeanFactory(@NotNull BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterPropertiesSet() {
        if (this.loaded) {
            return;
        }
        List<String> packages = ExtendPackagesProcessor.getPackagesWithApp(beanFactory, ScanConfig.class);
        // 处理配置的扫描处理器
        List<ScanProcessor> processorList = beanFactory.getBeanProvider(ScanProcessor.class).stream().toList();
        if (CollectionUtils.isEmpty(processorList)) {
            return;
        }
        ScanUtil.scanPackages(packages, processorList).forEach(ScanProcessorResult::processClasses);
        this.loaded = true;
    }

    public void doScan() {
        if (!this.loaded) {
            this.afterPropertiesSet();
        }
    }
}
