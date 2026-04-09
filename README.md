# Spring Extension Starter - Spring Boot 扩展组件库

<!--
Keywords: Spring Boot, Starter, Cache, I18N, Redis, REST Client, Tracing, UID, Logging, Documentation, Stream, Spring扩展, Spring Boot Starter
GitHub: https://github.com/Dao1230source/spring-extension-starter
Maven: io.github.dao1230source:spring-extension-starter-*
-->

> 一套企业级 Spring Boot 扩展组件，提供缓存增强、国际化、分布式锁、REST客户端、分布式追踪等开箱即用的能力。
>
> A collection of enterprise-grade Spring Boot extensions with ready-to-use features like enhanced caching, i18n,
> distributed locks, REST clients, and distributed tracing.

[![Java](https://img.shields.io/badge/Java-21+-green.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## 📦 模块总览

| 模块         | 说明                                  | 文档                                                                   |
|------------|-------------------------------------|----------------------------------------------------------------------|
| **common** | 基础工具模块，Jackson配置、SpEL工具、Spring上下文工具 | [spring-common.md](spring-extension-starter-common/spring-common.md) |
| **cache**  | Spring Cache扩展，批量缓存、二级缓存、分布式同步      | [spring-cache.md](spring-extension-starter-cache/spring-cache.md)    |
| **i18n**   | 国际化组件，枚举字典、JSON翻译、MessageFormat     | [spring-i18n.md](spring-extension-starter-i18n/spring-i18n.md)       |
| **io**     | 输入输出封装，统一响应格式、增强校验、分布式追踪集成          | [spring-io.md](spring-extension-starter-io/spring-io.md)             |
| **log**    | 方法执行日志、数据库变更日志、审计日志                 | [spring-log.md](spring-extension-starter-log/spring-log.md)          |
| **object** | 对象树管理，层级数据、多父节点、批量操作                | [spring-object.md](spring-extension-starter-object/spring-object.md) |
| **redis**  | Redis增强，分布式锁、Pub/Sub、类型安全操作         | [spring-redis.md](spring-extension-starter-redis/spring-redis.md)    |
| **rest**   | REST客户端，声明式接口、响应解包、追踪集成             | [spring-rest.md](spring-extension-starter-rest/spring-rest.md)       |
| **stream** | Spring Cloud Stream扩展，REST消息桥接      | [spring-stream.md](spring-extension-starter-stream/spring-stream.md) |
| **trace**  | 分布式追踪，TraceId传递、MDC集成               | [spring-trace.md](spring-extension-starter-trace/spring-trace.md)    |
| **uid**    | 分布式唯一ID生成，Yitter算法、Redis节点分配        | [spring-uid.md](spring-extension-starter-uid/spring-uid.md)          |
| **doc**    | API文档生成，JavaDoc解析、构建时生成             | [spring-doc.md](spring-extension-starter-doc/spring-doc.md)          |

---

## 🚀 快速开始

### Maven 依赖

按需引入模块：

```xml
<!-- 基础模块（其他模块的传递依赖） -->
<dependency>
    <groupId>io.github.dao1230source</groupId>
    <artifactId>spring-extension-starter-common</artifactId>
    <version>0.0.12</version>
</dependency>

        <!-- 缓存扩展 -->
<dependency>
<groupId>io.github.dao1230source</groupId>
<artifactId>spring-extension-starter-cache</artifactId>
<version>0.0.12</version>
</dependency>

        <!-- 国际化 -->
<dependency>
<groupId>io.github.dao1230source</groupId>
<artifactId>spring-extension-starter-i18n</artifactId>
<version>0.0.12</version>
</dependency>

        <!-- Redis增强 -->
<dependency>
<groupId>io.github.dao1230source</groupId>
<artifactId>spring-extension-starter-redis</artifactId>
<version>0.0.12</version>
</dependency>

        <!-- 其他模块类似... -->
```

### Gradle 依赖

```groovy
implementation 'io.github.dao1230source:spring-extension-starter-cache:0.0.12'
implementation 'io.github.dao1230source:spring-extension-starter-i18n:0.0.12'
implementation 'io.github.dao1230source:spring-extension-starter-redis:0.0.12'
```

---

## 📖 模块详解

### Cache - 缓存增强组件

基于 Spring Cache 的增强扩展：

- ✅ **批量缓存获取**：支持 Collection 入参的批量方法
- ✅ **二级缓存架构**：JVM 本地缓存 + Redis 远程缓存
- ✅ **分布式缓存同步**：Redis Pub/Sub 失效通知
- ✅ **部分缓存策略**：TRUST/DISTRUST/PARTIAL_TRUST
- ✅ **Redis 分片缓存**：FIXED_SHARD/FIXED_SIZE 策略，支持海量数据
- ✅ **参数化类型支持**：支持 Generic 类型的缓存存储和反序列化

```java

@EnableExtendedCache
@SpringBootApplication
public class Application {
}

@ConfigureCache(
        cacheNames = "users",
        key = "#ids",
        cacheKeySpEl = "#R.id",
        cacheInJvm = @CacheInJvm(enable = true),
        cacheInRedis = @CacheInRedis(valueClasses = {User.class})
)
public List<User> getUsersByIds(Collection<String> ids) {
}

// Redis 分片缓存示例
@ConfigureCache(
        cacheNames = "products",
        key = "#ids",
        cacheKeySpEl = "#R.id",
        cacheInRedis = @CacheInRedis(
                shardStrategy = ShardStrategyEnum.FIXED_SHARD,
                shardValue = 16,
                valueClasses = {Product.class}
        )
)
public List<Product> getProductsByIds(Collection<String> ids) {
}
```

📄 [完整文档](spring-extension-starter-cache/spring-cache.md)

---

### I18N - 国际化组件

开箱即用的国际化解决方案：

- ✅ **枚举字典自动注册**：`@I18nServer` 标注的枚举自动加载
- ✅ **JSON 自动翻译**：`@I18n` 标注字段自动翻译输出
- ✅ **内存缓存**：高性能 JVM 缓存
- ✅ **MessageFormat 支持**：参数化消息

```java

@EnableExtendedI18n
@SpringBootApplication
public class Application {
}

@I18nServer(group = "status")
public enum Status {
    ENABLED("enabled", "启用"),
    DISABLED("disabled", "禁用")
}

public class UserDTO {
    @I18n(group = "status")
    private String status;  // 自动翻译
}
```

📄 [完整文档](spring-extension-starter-i18n/spring-i18n.md)

---

### Redis - Redis 增强组件

增强的 Redis 操作能力：

- ✅ **声明式分布式锁**：`@Lock` 注解，支持批量锁和 SpEL
- ✅ **Pub/Sub 消息**：接口驱动的消息发布订阅
- ✅ **类型安全操作**：泛型支持的 RedisTemplate
- ✅ **JSON 序列化**：自动序列化/反序列化

```java

@Lock("'order:' + #orderId")
public OrderDTO createOrder(String orderId) {
}

@Component
@RedisListener(channel = "order-events")
public class OrderHandler implements MessageDelegate {
    @Override
    public void handleMessage(String message) {
    }
}
```

📄 [完整文档](spring-extension-starter-redis/spring-redis.md)

---

### Rest - REST 客户端组件

声明式 REST 客户端（类似 OpenFeign）：

- ✅ **接口定义 API**：Java 接口定义 REST API
- ✅ **自动响应解包**：`Output<T>` 自动解包为 `T`
- ✅ **分布式追踪集成**：自动注入 TraceId

```java

@EnableExtendedRest
@SpringBootApplication
public class Application {
}

@Rest(name = "user-service")
public interface UserApi {
    @GET("/users/{id}")
    UserDTO getUser(@Path("id") String id);
}
```

📄 [完整文档](spring-extension-starter-rest/spring-rest.md)

---

### Trace - 分布式追踪组件

轻量级分布式追踪：

- ✅ **Trace ID 传递**：同步/异步线程间自动传递
- ✅ **MDC 集成**：Log4j2 无缝集成
- ✅ **零配置**：添加依赖即生效

```java
public void traceId() {
    TraceContext.setTraceId("abc123");
    String traceId = TraceContext.getTraceId();
}
```

📄 [完整文档](spring-extension-starter-trace/spring-trace.md)

---

### UID - 分布式唯一ID生成

基于 Yitter 算法的分布式 ID 生成器：

- ✅ **全局唯一**：多实例部署保证不冲突
- ✅ **时间有序**：ID 按时间递增
- ✅ **高性能**：单机百万级 QPS
- ✅ **Redis 节点分配**：自动分配 WorkerId

```java
public void uid() {
    long id = Uids.nextId();
    String strId = Uids.nextStrId("ORD");  // ORD1234567890123456789
}

```

📄 [完整文档](spring-extension-starter-uid/spring-uid.md)

---

### IO - 输入输出封装组件

统一的请求响应处理：

- ✅ **统一请求封装**：`Input<T>` 带验证支持
- ✅ **统一响应封装**：`Output<T>` 标准响应格式
- ✅ **枚举校验**：`@EnumExists` 验证枚举值存在性
- ✅ **操作分组校验**：Add/Update/Delete/Select 分组

```java

@PostMapping("/users")
public Output<UserDTO> create(
        @RequestBody @Validated(Add.class) Input<UserForm> input
) {
    return Output.success(userService.create(input.getData()));
}
```

📄 [完整文档](spring-extension-starter-io/spring-io.md)

---

### Log - 方法执行日志组件

AOP 驱动的日志记录：

- ✅ **方法执行日志**：`@Log` 记录入参、返回值、执行时间
- ✅ **数据库变更日志**：`@DataSourceLog` 自动捕获 SQL 变更
- ✅ **SpEL 支持**：动态生成日志内容
- ✅ **自定义处理器**：支持持久化到数据库

```java

@Log("'创建订单: ' + #orderId")
@DataSourceLog(tables = {"orders"})
public OrderDTO createOrder(String orderId) {
}
```

📄 [完整文档](spring-extension-starter-log/spring-log.md)

---

### Object - 对象树管理组件

层级对象数据管理：

- ✅ **树形结构管理**：父子关系、多父节点（DAG）
- ✅ **批量操作**：批量保存、合并、删除
- ✅ **关系管理**：多种关系类型和作用域
- ✅ **状态追踪**：追踪对象状态变化

📄 [完整文档](spring-extension-starter-object/spring-object.md)

---

### Stream - 消息流扩展组件

Spring Cloud Stream REST 扩展：

- ✅ **REST 消息桥接**：HTTP 请求作为消息处理
- ✅ **声明式生产者/消费者**：接口定义消息通道
- ✅ **动态配置**：运行时启用/禁用

📄 [完整文档](spring-extension-starter-stream/spring-stream.md)

---

### Doc - API 文档生成组件

JavaDoc 驱动的文档生成：

- ✅ **源码解析**：自定义 Doclet 解析 Java 源码
- ✅ **构建时生成**：无运行时性能影响
- ✅ **继承关系处理**：自动合并父类文档

📄 [完整文档](spring-extension-starter-doc/spring-doc.md)

---

## 🏷️ 标签

**Keywords**: Spring Boot, Starter, Cache, I18N, Redis, Distributed Lock, REST Client, Tracing, UID, Logging,
Documentation, Stream, Pub/Sub, JSON Serialization, Distributed System, Microservices

**关键词**: Spring扩展, Spring Boot Starter, 缓存增强, 国际化, 分布式锁, REST客户端, 链路追踪, 唯一ID, 操作日志, API文档,
消息流, 发布订阅, JSON序列化, 分布式系统, 微服务

---

## 📚 资源

- **GitHub 仓库**: https://github.com/Dao1230source/spring-extension-starter
- **示例代码**: https://github.com/Dao1230source/demo
- **问题反馈**: https://github.com/Dao1230source/spring-extension-starter/issues

---

## 📄 许可证

MIT License