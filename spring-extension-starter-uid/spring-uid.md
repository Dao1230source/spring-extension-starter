# Spring Extension UID - 分布式唯一ID生成组件

> **关键词（Keywords）**: Unique ID, Snowflake, Yitter ID, Distributed ID Generator, Time-Sortable ID, Cluster Node Assignment, 唯一ID, 雪花算法, 分布式ID, 时间有序

[![Maven Central](https://img.shields.io/maven-central/v/io.github.dao1230source/spring-extension-starter-uid.svg)](https://central.sonatype.com/artifact/io.github.dao1230source/spring-extension-starter-uid)
[![Java](https://img.shields.io/badge/Java-21+-green.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)

## 概述

基于 Yitter 算法（雪花算法改进版）的分布式唯一 ID 生成器，提供：

- **全局唯一**：多实例部署时保证 ID 不冲突
- **时间有序**：ID 按时间递增，支持按 ID 排序
- **高性能**：单机每秒生成百万级 ID
- **Redis 节点分配**：自动通过 Redis 分配 WorkerId

**适用于分布式系统、微服务架构的主键生成场景**。

---

## 快速开始

### 1. 添加依赖

**Maven:**

```xml
<dependency>
    <groupId>io.github.dao1230source</groupId>
    <artifactId>spring-extension-starter-uid</artifactId>
    <version>0.0.12</version>
</dependency>
```

### 2. 配置 Redis

```properties
spring.redis.host=localhost
spring.redis.port=6379
```

### 3. 使用 ID 生成器

```java
import org.source.spring.uid.Uids;

@Service
public class OrderService {
    
    public Order createOrder() {
        // 生成长整型 ID
        long id = Uids.nextId();
        
        // 生成字符串 ID
        String strId = Uids.nextStrId();
        
        // 生成带前缀的字符串 ID
        String prefixedId = Uids.nextStrId("ORD");
        // 输出: ORD1234567890123456789
        
        Order order = new Order();
        order.setId(id);
        return order;
    }
}
```

---

## ID 结构

### 长整型 ID（64位）

```
| 1位符号位 | 41位时间戳 | 10位WorkerId | 12位序列号 |
```

- **时间戳**：毫秒级，可用约 69 年
- **WorkerId**：自动通过 Redis 分配，支持 1024 个节点
- **序列号**：同一毫秒内的序列，支持 4096 个 ID

### 字符串 ID

- 长整型 ID 转换为字符串
- 可选添加业务前缀

---

## 自定义配置

### 配置属性

```properties
# ID 生成器配置
org.source.spring.uid.startDate=2024-01-01   # 起始日期
org.source.spring.uid.workerIdBits=10         # WorkerId 位数
org.source.spring.uid.sequenceBits=12         # 序列号位数
```

### 自定义前缀

实现 `UidPrefix` 接口：

```java
public enum OrderPrefix implements UidPrefix {
        ORDER("ORD", "订单ID"),
        PAYMENT("PAY", "支付ID");
    
        private final String prefix;
        private final String description;
    
        // constructor & getters
}

    // 使用
    String orderId = Uids.nextStrId(OrderPrefix.ORDER);
    // 输出: ORD1234567890123456789
```

---

## 节点自动分配

### Redis 分配机制

模块启动时自动通过 Redis 分配 WorkerId：

```java
public class Example {
    public void example() {
        // 自动执行
        // 1. 检查 Redis 中已分配的 WorkerId
        // 2. 分配一个未使用的 WorkerId
        // 3. 注册心跳，保持占用
    }
}
```

### 节点重启处理

- 节点重启时重新分配新的 WorkerId
- 旧 WorkerId 通过心跳超时自动释放
- 保证不重复分配

---

## 核心类说明

| 类名 | 说明 |
|------|------|
| `Uids` | ID 生成工具类，提供静态方法 |
| `UidPrefix` | ID 前缀定义接口 |
| `UidProperties` | ID 生成器配置属性 |
| `UidConfig` | 自动配置类 |

---

## 使用场景

### 场景1：订单 ID 生成

```java
@Service
public class OrderService {
    
    public Order createOrder(OrderForm form) {
        Order order = new Order();
        order.setId(Uids.nextId());
        order.setOrderNo(Uids.nextStrId("ORD"));
        return orderRepository.save(order);
    }
}
```

### 场景2：分布式主键

```java
@Entity
public class User {
    @Id
    private Long id;  // 使用 Uids.nextId() 生成
    
    private String name;
}
```

### 场景3：追踪 ID

```java
@Component
public class TraceFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        String traceId = Uids.nextStrId("TRC");
        TraceContext.setTraceId(traceId);
        chain.doFilter(request, response);
    }
}
```

---

## 性能特性

| 指标 | 数值 |
|------|------|
| 单机 QPS | > 100万/秒 |
| 响应时间 | < 1μs |
| 时钟回拨处理 | 等待/拒绝 |

---

## 与其他方案对比

| 特性 | 本模块 | UUID | 数据库自增 |
|------|--------|------|------------|
| 全局唯一 | ✅ | ✅ | ⚠️ 需配置 |
| 时间有序 | ✅ | ❌ | ✅ |
| 性能 | ✅ 极高 | ✅ 高 | ⚠️ 低 |
| 分布式支持 | ✅ | ✅ | ❌ |

---

## 相关资源

- **GitHub 仓库**: [spring-extension-starter](https://github.com/Dao1230source/spring-extension-starter)
- **示例代码**: [Demo Project](https://github.com/Dao1230source/demo)
- **问题反馈**: [GitHub Issues](https://github.com/Dao1230source/spring-extension-starter/issues)

---

## 标签（Tags）

`unique-id` `snowflake` `yitter-id` `distributed-id-generator` `time-sortable-id` `cluster-node-assignment` `spring-boot-starter` `唯一ID` `雪花算法` `分布式ID`