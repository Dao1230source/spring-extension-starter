# Spring Extension Trace - 分布式追踪组件

> **关键词（Keywords）**: Distributed Tracing, Trace ID, MDC, TransmittableThreadLocal, Thread Context, Log4j2, Async Context, 分布式追踪, 链路追踪, 线程上下文, 异步传递

[![Maven Central](https://img.shields.io/maven-central/v/io.github.dao1230source/spring-extension-starter-trace.svg)](https://central.sonatype.com/artifact/io.github.dao1230source/spring-extension-starter-trace)
[![Java](https://img.shields.io/badge/Java-21+-green.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)

## 概述

轻量级分布式追踪组件，提供：

- **Trace ID 传递**：自动在同步/异步线程间传递追踪上下文
- **MDC 集成**：与 Log4j2 的 MDC 无缝集成
- **上下文管理**：统一管理 TraceId、UserId、SpaceId 等
- **TransmittableThreadLocal**：支持线程池场景的上下文传递

**无需额外配置**，添加依赖即可自动生效。

---

## 快速开始

### 1. 添加依赖

**Maven:**

```xml
<dependency>
    <groupId>io.github.dao1230source</groupId>
    <artifactId>spring-extension-starter-trace</artifactId>
    <version>0.0.12</version>
</dependency>
```

### 2. 自动生效

模块通过 `log4j2.component.properties` 自动配置，无需手动启用。

---

## 核心功能

### TraceContext 工具类

管理追踪上下文数据：

```java
import org.source.spring.trace.TraceContext;

public class Example {
    public void example() {

        // 设置 TraceId
        TraceContext.setTraceId("abc123");

        // 获取 TraceId
        String traceId = TraceContext.getTraceId();

        // 设置用户ID
        TraceContext.setUserId("user001");

        // 设置扩展数据
        TraceContext.setExtension("customKey", "customValue");

        // 清除上下文
        TraceContext.clear();
    }
}
```

### 支持的上下文数据

| Key | 说明 |
|-----|------|
| `TRACE_ID` | 追踪ID |
| `USER_ID` | 用户ID |
| `SPACE_ID` | 租户ID |
| `SECRET_KEY` | 安全密钥 |
| Extension | 自定义扩展数据 |

### 在日志中使用

配置 Log4j2 日志格式：

```xml
<PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss} [%X{TRACE_ID}] [%thread] %-5level %logger{36} - %msg%n"/>
```

日志输出示例：

```
2024-03-01 10:30:00 [abc123] [http-nio-8080-exec-1] INFO  com.example.UserService - User login
```

---

## 线程池场景支持

### TransmittableThreadLocal 实现

模块使用 `TtlThreadContextMap` 替代默认的 ThreadContextMap：

```java
public class Example {
    public void example() {
        // 同步传递到异步线程
        ExecutorService executor = Executors.newFixedThreadPool(10);

        // 使用 TtlExecutors 包装
        ExecutorService ttlExecutor = TtlExecutors.getTtlExecutorService(executor);

        TraceContext.setTraceId("parent-trace-id");

        ttlExecutor.submit(() -> {
            // 异步线程中可获取 TraceId
            String traceId = TraceContext.getTraceId();  // "parent-trace-id"
        });
    }
}
```

---

## 使用场景

### 场景1：Web 请求追踪

```java
@Component
public class TraceFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        try {
            // 从请求头获取或生成 TraceId
            String traceId = ((HttpServletRequest) request).getHeader("X-Trace-Id");
            if (traceId == null) {
                traceId = UUID.randomUUID().toString();
            }
            TraceContext.setTraceId(traceId);
            
            chain.doFilter(request, response);
        } finally {
            TraceContext.clear();
        }
    }
}
```

### 场景2：异步任务追踪

```java
@Service
public class NotificationService {
    
    @Async
    public void sendNotification(String userId, String message) {
        // TraceId 自动传递到异步线程
        String traceId = TraceContext.getTraceId();
        log.info("Sending notification to user: {}", userId);
    }
}
```

### 场景3：微服务调用追踪

```java
@Component
public class TraceInterceptor implements ClientHttpRequestInterceptor {
    
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) {
        // 传递 TraceId 到下游服务
        request.getHeaders().set("X-Trace-Id", TraceContext.getTraceId());
        return execution.execute(request, body);
    }
}
```

---

## 核心类说明

| 类名 | 说明 |
|------|------|
| `TraceContext` | 上下文管理工具类 |
| `TtlThreadContextMap` | 基于 TTL 的 ThreadContextMap 实现 |

### 自动配置文件

`log4j2.component.properties`:

```properties
log4j2.ThreadContextMap=org.source.spring.trace.TtlThreadContextMap
```

---

## 与其他方案对比

| 特性 | 本模块 | Spring Cloud Sleuth | MDC 手动管理 |
|------|--------|---------------------|--------------|
| 自动配置 | ✅ 零配置 | ⚠️ 需配置 | ❌ 需手动 |
| 异步传递 | ✅ TTL 支持 | ✅ 支持 | ❌ 需手动 |
| 轻量级 | ✅ 极简 | ⚠️ 较重 | ✅ 轻量 |
| 功能完整度 | ⚠️ 基础 | ✅ 完整 | ⚠️ 基础 |

---

## 相关资源

- **GitHub 仓库**: [spring-extension-starter](https://github.com/Dao1230source/spring-extension-starter)
- **示例代码**: [Demo Project](https://github.com/Dao1230source/demo)
- **问题反馈**: [GitHub Issues](https://github.com/Dao1230source/spring-extension-starter/issues)

---

## 标签（Tags）

`distributed-tracing` `trace-id` `mdc` `transmittable-thread-local` `thread-context` `log4j2` `async-context` `spring-boot-starter` `分布式追踪` `链路追踪` `线程上下文`