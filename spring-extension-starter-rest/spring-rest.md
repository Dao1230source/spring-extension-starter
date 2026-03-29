# Spring Extension Rest - REST 客户端组件

> **关键词（Keywords）**: REST Client, Retrofit, Declarative HTTP Client, OkHttp, Tracing, Request Wrapper, Response Unwrapper, REST客户端, 声明式HTTP客户端, 请求封装, 响应解包

[![Maven Central](https://img.shields.io/maven-central/v/io.github.dao1230source/spring-extension-starter-rest.svg)](https://central.sonatype.com/artifact/io.github.dao1230source/spring-extension-starter-rest)
[![Java](https://img.shields.io/badge/Java-21+-green.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)

## 概述

基于 Retrofit 的声明式 REST 客户端，类似 OpenFeign，提供：

- **接口定义 API**：通过 Java 接口定义 REST API
- **自动响应解包**：自动将 `Output<T>` 解包为 `T`
- **请求自动封装**：可选将请求自动封装为 `Input<T>`
- **分布式追踪集成**：自动注入 TraceId、UserId 等请求头

**无需手写 HTTP 调用代码**，接口即实现。

---

## 快速开始

### 1. 添加依赖

**Maven:**

```xml
<dependency>
    <groupId>io.github.dao1230source</groupId>
    <artifactId>spring-extension-starter-rest</artifactId>
    <version>0.0.12</version>
</dependency>
```

### 2. 启用 REST 客户端

```java
@SpringBootApplication
@EnableExtendedRest
public class Application {
    static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. 定义 REST 接口

```java
@Rest(name = "user-service")
public interface UserApi {
    
    @GET("/users/{id}")
    UserDTO getUser(@Path("id") String id);
    
    @POST("/users")
    UserDTO createUser(@Body UserForm form);
    
    @GET("/users")
    List<UserDTO> listUsers(@Query("status") String status);
}
```

### 4. 配置服务地址

```properties
org.source.spring.rests.user-service.baseUrl=http://user-service:8080
org.source.spring.rests.user-service.readTimeout=5000
```

### 5. 使用接口

```java
@Service
public class OrderService {
    
    @Autowired
    private UserApi userApi;
    
    public OrderDTO createOrder(String userId) {
        UserDTO user = userApi.getUser(userId);  // 自动调用远程服务
        // ...
    }
}
```

---

## 核心功能

### 自动响应解包

当远程服务返回 `Output<UserDTO>` 格式时，自动解包：

```java
public class Example {
    // 远程服务返回: {"code":"SUCCESS","data":{"id":"1","name":"张三"}}
    // 接口直接返回 UserDTO
    @GET("/users/{id}")
    UserDTO getUser(@Path("id") String id);
}
```

### 请求自动封装

可选将请求参数自动封装为 `Input<T>`：

```properties
# 启用请求封装
org.source.spring.rests.user-service.autoPackRequest=true
```

```java
public class Example {
    // 接口定义
    @POST("/users")
    UserDTO createUser(@Body UserForm form);

    // 实际发送: {"data":{"name":"张三","email":"test@example.com"}}
}
```

### 分布式追踪

自动注入请求头：

| Header | 说明 |
|--------|------|
| `X-Trace-Id` | 追踪ID |
| `X-User-Id` | 用户ID |
| `X-Space-Id` | 租户ID |
| `X-Secret-Key` | 安全密钥 |

---

## 配置属性

```properties
# 基础配置
org.source.spring.rests.{name}.baseUrl=http://example.com
org.source.spring.rests.{name}.readTimeout=5000
org.source.spring.rests.{name}.connectTimeout=3000

# 高级配置
org.source.spring.rests.{name}.autoUnpackResponse=true
org.source.spring.rests.{name}.autoPackRequest=false
org.source.spring.rests.{name}.secretKey=your-secret
```

---

## 使用场景

### 场景1：微服务间调用

```java
@Rest(name = "order-service")
public interface OrderApi {
    
    @GET("/orders/{orderId}")
    OrderDTO getOrder(@Path("orderId") String orderId);
    
    @POST("/orders")
    OrderDTO createOrder(@Body OrderForm form);
}

@Service
public class PaymentService {
    @Autowired
    private OrderApi orderApi;
    
    public void processPayment(String orderId) {
        OrderDTO order = orderApi.getOrder(orderId);
        // 处理支付
    }
}
```

### 场景2：调用第三方 API

```java
@Rest(name = "weather-api")
public interface WeatherApi {
    
    @GET("/weather")
    WeatherData getWeather(@Query("city") String city, @Query("key") String apiKey);
}
```

### 场景3：批量操作

```java
@Rest(name = "user-service")
public interface UserApi {
    
    @POST("/users/batch")
    List<UserDTO> batchCreate(@Body List<UserForm> forms);
    
    @GET("/users")
    PageResult<UserDTO> listUsers(@QueryMap Map<String, Object> params);
}
```

---

## 与 OpenFeign 对比

| 特性 | 本模块 | OpenFeign |
|------|--------|-----------|
| 声明式接口 | ✅ | ✅ |
| 响应自动解包 | ✅ 内置 | ⚠️ 需配置 |
| 请求自动封装 | ✅ 内置 | ❌ 不支持 |
| Retrofit 生态 | ✅ 完整支持 | ❌ 不支持 |
| Spring Cloud 集成 | ⚠️ 基础支持 | ✅ 深度集成 |

---

## 核心类说明

| 类名 | 说明 |
|------|------|
| `EnableExtendedRest` | 启用 REST 扫描 |
| `Rest` | 接口标注注解 |
| `RestInterfaceScanner` | 接口扫描器 |
| `AdviceJacksonConverterFactory` | 增强 JSON 转换器 |
| `CallAdapterFactory` | 同步调用适配器 |
| `RestInterceptor` | 请求拦截器，注入追踪头 |

---

## 相关资源

- **GitHub 仓库**: [spring-extension-starter](https://github.com/Dao1230source/spring-extension-starter)
- **示例代码**: [Demo Project](https://github.com/Dao1230source/demo)
- **问题反馈**: [GitHub Issues](https://github.com/Dao1230source/spring-extension-starter/issues)

---

## 标签（Tags）

`rest-client` `retrofit` `declarative-http` `okhttp` `tracing` `request-wrapper` `response-unwrapper` `spring-boot-starter` `REST客户端` `声明式HTTP`