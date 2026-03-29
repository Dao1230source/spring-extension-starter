# Spring Extension Common - Spring 通用基础组件

> **关键词（Keywords）**: Spring Boot, Jackson, SpEL, Spring Expression Language, ApplicationContext, Bean Management, Dynamic Bean Registration, JSON Serialization, 弹簧表达式, Bean管理, 动态注册

[![Maven Central](https://img.shields.io/maven-central/v/io.github.dao1230source/spring-extension-starter-common.svg)](https://central.sonatype.com/artifact/io.github.dao1230source/spring-extension-starter-common)
[![Java](https://img.shields.io/badge/Java-21+-green.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)

## 概述

Spring Extension Starter 系列的基础模块，提供通用的工具类和配置，包括：

- **Jackson 自动配置**：统一的 JSON 序列化配置
- **SpEL 表达式增强**：扩展的 Spring Expression Language 解析能力
- **Spring 上下文工具**：ApplicationContext 访问和 Bean 管理工具
- **动态 Bean 注册**：抽象的 ImportRegistrar 基类
- **安全配置属性**：统一的安全相关配置管理

**作为其他 starter 的基础依赖**，本模块提供了跨模块共享的核心能力。

---

## 核心功能

### 1. Jackson 配置（JacksonConfig）

自动配置主 `ObjectMapper`，提供统一的 JSON 处理能力：

```java
public class Example {
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        // 自动配置的 ObjectMapper
}
}
```

### 2. SpEL 表达式工具（SpElUtil）

扩展的 Spring Expression Language 解析能力：

```java
public class Example {
    public void example() {
        // 解析 SpEL 表达式
        Object result = SpElUtil.parse(expression, context);

        // 使用扩展的表达式求值器
        ExtendExpressionEvaluator evaluator = new ExtendExpressionEvaluator();
    }
}
```

### 3. Spring 上下文工具（SpringUtil）

便捷访问 Spring ApplicationContext：

```java
public class Example {
    public void example() {
        // 获取 Bean
        MyBean bean = SpringUtil.getBean(MyBean.class);

        // 获取 ApplicationContext
        ApplicationContext ctx = SpringUtil.getApplicationContext();
    }
}
```

### 4. 动态 Bean 注册（AbstractImportRegistrar）

为其他模块提供动态 Bean 注册的抽象基类：

```java
public class MyImportRegistrar extends AbstractImportRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        // 动态注册 Bean
    }
}
```

### 5. Bean 操作工具（BeanUtil）

Bean 属性复制和操作：

```java
public class Example {
    public void example() {
        // 属性复制
        BeanUtil.copyProperties(source, target);
    }
}
```

---

## 快速开始

### 添加依赖

**Maven:**

```xml
<dependency>
    <groupId>io.github.dao1230source</groupId>
    <artifactId>spring-extension-starter-common</artifactId>
    <version>0.0.12</version>
</dependency>
```

**Gradle:**

```groovy
implementation 'io.github.dao1230source:spring-extension-starter-common:0.0.12'
```

### 使用示例

#### 使用 SpEL 表达式

```java
import org.source.spring.common.spel.SpElUtil;

public class Example {
    public void example() {

        // 解析简单表达式
        Object result = SpElUtil.parse("#user.name", context);

        // 使用扩展变量
        ExtendEvaluationContext context = new ExtendEvaluationContext(rootObject);
        context.setVariable("customVar", value);
    }
}
```

#### 获取 Spring Bean

```java
import org.source.spring.common.utility.SpringUtil;

@Service
public class MyService {
    public void doSomething() {
        // 动态获取 Bean
        OtherService other = SpringUtil.getBean(OtherService.class);
    }
}
```

---

## 核心类说明

| 类名 | 说明 |
|------|------|
| `JacksonConfig` | Jackson ObjectMapper 自动配置 |
| `SpElUtil` | SpEL 表达式解析工具 |
| `ExtendExpressionEvaluator` | 扩展的表达式求值器 |
| `ExtendEvaluationContext` | 扩展的表达式上下文 |
| `SpringUtil` | Spring 上下文工具类 |
| `BeanUtil` | Bean 操作工具类 |
| `AbstractImportRegistrar` | 动态 Bean 注册基类 |
| `SecurityProperties` | 安全配置属性 |

---

## 适用场景

- 需要统一 JSON 序列化配置的项目
- 需要动态 SpEL 表达式解析的场景
- 需要在非 Spring 管理类中获取 Bean
- 开发自定义 Spring Boot Starter
- 需要动态注册 Bean 的场景

---

## 与其他模块的关系

本模块是 Spring Extension Starter 系列的**基础依赖**：

```
spring-extension-starter-common (基础)
├── spring-extension-starter-cache (依赖 common)
├── spring-extension-starter-i18n (依赖 common)
├── spring-extension-starter-log (依赖 common)
├── spring-extension-starter-rest (依赖 common)
└── ... 其他模块
```

---

## 相关资源

- **GitHub 仓库**: [spring-extension-starter](https://github.com/Dao1230source/spring-extension-starter)
- **示例代码**: [Demo Project](https://github.com/Dao1230source/demo)
- **问题反馈**: [GitHub Issues](https://github.com/Dao1230source/spring-extension-starter/issues)

---

## 标签（Tags）

`spring-boot` `spring-boot-starter` `jackson` `spel` `spring-expression` `application-context` `bean-management` `dynamic-registration` `json-serialization` `spring-common` `Spring基础组件` `Bean管理` `动态注册`