# Spring Extension I18N - 国际化组件

> **关键词（Keywords）**: I18N, Internationalization, Localization, Multi-language, Enum Translation, Jackson Serialization, Message Format, 国际化, 多语言, 枚举翻译, JSON序列化翻译

[![Maven Central](https://img.shields.io/maven-central/v/io.github.dao1230source/spring-extension-starter-i18n.svg)](https://central.sonatype.com/artifact/io.github.dao1230source/spring-extension-starter-i18n)
[![Java](https://img.shields.io/badge/Java-21+-green.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)

## 概述

开箱即用的 Spring Boot 国际化（I18N）组件，提供：

- **内存字典管理**：基于 JVM 缓存的高性能字典存储
- **枚举自动加载**：标注 `@I18nServer` 的枚举自动注册为字典
- **JSON 序列化翻译**：标注 `@I18n` 的字段自动翻译输出
- **静态工具类**：便捷的静态方法访问国际化数据
- **MessageFormat 支持**：支持参数化消息格式化

**无需外部配置文件**，直接在代码中定义字典数据，适合动态管理的国际化场景。

---

## 快速开始

### 1. 添加依赖

**Maven:**

```xml
<dependency>
    <groupId>io.github.dao1230source</groupId>
    <artifactId>spring-extension-starter-i18n</artifactId>
    <version>0.0.12</version>
</dependency>
```

### 2. 启用 I18N

```java
@SpringBootApplication
@EnableExtendedI18n
public class Application {
    static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. 定义字典枚举

```java
@I18nServer(group = "status")
public enum StatusEnum implements EnumProcessor<I18nException> {
    ENABLED("enabled", "启用"),
    DISABLED("disabled", "禁用");

    private final String code;
    private final String value;

    // 枚举会自动注册到 I18N 字典
}
```

### 4. 在响应中自动翻译

```java
@Data
public class UserDTO {
    private String name;
    
    @I18n  // 自动翻译此字段
    private String status;
}
    // 输出时 status 的值会自动翻译为对应语言的文本
```

---

## 核心注解

### @EnableExtendedI18n

启用 I18N 扩展，导入必要的配置：

```java
@EnableExtendedI18n
@SpringBootApplication
public class Application { }
```

### @I18nServer

标注在枚举上，将其注册为字典数据源：

```java
@I18nServer(group = "gender")  // 指定分组
public enum GenderEnum {
    MALE("male", "男"),
    FEMALE("female", "女")
}
```

### @I18n

标注在字段上，JSON 序列化时自动翻译：

```java
public class OrderDTO {
    @I18n(group = "status")  // 指定字典分组
    private String status;
}
```

### @I18nRef

指定如何从对象或字段提取 I18N Key：

```java
public class OrderDTO {
    @I18n
    @I18nRef(field = "statusCode")  // 从 statusCode 字段提取 key
    private String statusText;
}
```

---

## 核心类说明

| 类名 | 说明 |
|------|------|
| `I18nTemplate` | 核心服务类，提供字典的增删改查操作 |
| `I18nWrapper` | 静态工具类，便捷访问国际化功能 |
| `I18nProcessor` | 字典处理接口，可自定义存储实现 |
| `DefaultI18nProcessor` | 默认内存实现，使用树结构存储 |
| `I18nSerializer` | Jackson 序列化器，翻译 @I18n 字段 |

---

## 使用场景

### 场景1：枚举字典自动注册

```java
@I18nServer(group = "order_status")
@Getter
@AllArgsConstructor
public enum OrderStatus {
    PENDING("pending", "待处理"),
    PROCESSING("processing", "处理中"),
    COMPLETED("completed", "已完成"),
    CANCELLED("cancelled", "已取消");
    
    private final String code;
    private final String desc;
}

    // 自动注册后，可通过 I18nWrapper 获取
    String text = I18nWrapper.getValue("order_status", "pending", "zh_CN");
    // 返回: "待处理"
```

### 场景2：API 响应自动翻译

```java
@Data
public class OrderDTO {
    private String orderId;
    
    @I18n(group = "order_status")
    private String status;  // 原始值: "pending" → 输出: "待处理"
}

    // Controller 返回时自动翻译
    @GetMapping("/orders/{id}")
    public OrderDTO getOrder(@PathVariable String id) {
    OrderDTO order = orderService.findById(id);
    order.setStatus("pending");  // 存储的是 code
    return order;  // JSON 输出时 status 被翻译为 "待处理"
}
```

### 场景3：参数化消息

```java
public class Example {
    public void example() {
        // 支持带参数的国际化消息
        String message = I18nWrapper.format(
            "messages", 
            "welcome", 
            Locale.CHINA,
            "张三"  // 参数
        );
        // 消息模板: "欢迎您，{0}！"
        // 输出: "欢迎您，张三！"
    }
}
```

### 场景4：自定义存储实现

```java
@Component
public class DatabaseI18nProcessor implements I18nProcessor {
    
    @Override
    public Map<String, String> findByGroup(String group, Locale locale) {
        // 从数据库加载字典
        return i18nRepository.findByGroupAndLocale(group, locale);
    }
    
    @Override
    public void save(String group, String key, String value, Locale locale) {
        // 保存到数据库
    }
}
```

---

## 配置属性

无需额外配置，默认使用内存存储。如需自定义：

```properties
# 可扩展配置
i18n.default-locale=zh_CN
i18n.cache.enabled=true
```

---

## 与其他方案对比

| 特性 | 本模块 | Spring MessageSource | ResourceBundle |
|------|--------|---------------------|----------------|
| 枚举字典支持 | ✅ 自动注册 | ❌ 需手动配置 | ❌ 需手动配置 |
| JSON 翻译 | ✅ 自动 | ❌ 需手动处理 | ❌ 需手动处理 |
| 动态更新 | ✅ 支持运行时更新 | ❌ 需重启 | ❌ 需重启 |
| 内存缓存 | ✅ JVM 缓存 | ⚠️ 可选 | ❌ 无 |
| MessageFormat | ✅ 支持 | ✅ 支持 | ✅ 支持 |

---

## 适用场景

- ✅ 需要枚举值自动翻译的 API 响应
- ✅ 需要运行时动态更新字典
- ✅ 不希望维护外部 properties 文件
- ✅ 需要高性能的内存字典
- ✅ 多语言业务数据展示

---

## 相关资源

- **GitHub 仓库**: [spring-extension-starter](https://github.com/Dao1230source/spring-extension-starter)
- **示例代码**: [Demo Project](https://github.com/Dao1230source/demo)
- **问题反馈**: [GitHub Issues](https://github.com/Dao1230source/spring-extension-starter/issues)

---

## 标签（Tags）

`i18n` `internationalization` `localization` `multi-language` `enum-translation` `jackson-serialization` `message-format` `spring-boot-starter` `国际化` `多语言` `枚举翻译` `JSON翻译`