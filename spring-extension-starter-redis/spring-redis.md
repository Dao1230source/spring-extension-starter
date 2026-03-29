# Spring Extension Redis - Redis 增强组件

> **关键词（Keywords）**: Redis, Distributed Lock, Pub/Sub, Redisson, Type-Safe Template, Batch Lock, JSON Serialization, 分布式锁, 发布订阅, 类型安全, 批量锁

[![Maven Central](https://img.shields.io/maven-central/v/io.github.dao1230source/spring-extension-starter-redis.svg)](https://central.sonatype.com/artifact/io.github.dao1230source/spring-extension-starter-redis)
[![Java](https://img.shields.io/badge/Java-21+-green.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)

## 概述

增强的 Redis 操作组件，提供：

- **分布式锁（@Lock）**：声明式分布式锁，支持批量锁和 SpEL 动态 Key
- **Pub/Sub 消息**：接口驱动的消息发布订阅
- **类型安全操作**：泛型支持的 RedisTemplate
- **JSON 序列化**：自动 JSON 序列化/反序列化

**简化 Redis 开发**，无需手写加锁/解锁逻辑，无需手动序列化。

---

## 快速开始

### 1. 添加依赖

**Maven:**

```xml
<dependency>
    <groupId>io.github.dao1230source</groupId>
    <artifactId>spring-extension-starter-redis</artifactId>
    <version>0.0.12</version>
</dependency>
```

### 2. 配置 Redis

```properties
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=

# 启用分布式锁
org.source.spring.enabled.lock=true
```

---

## 分布式锁（@Lock）

### 基本用法

```java
@Service
public class OrderService {
    
    @Lock("#orderId")  // 使用参数作为锁 Key
    public OrderDTO createOrder(String orderId) {
        // 同一 orderId 同时只有一个线程能执行
        return orderRepository.save(orderId);
    }
}
```

### SpEL 表达式支持

```java
@Service
public class InventoryService {
    
    @Lock("'inventory:' + #productId")
    public void deductStock(String productId, int quantity) {
        // 动态生成锁 Key
        inventoryRepository.deduct(productId, quantity);
    }
    
    @Lock("'user:' + #user.id + ':order'")
    public OrderDTO createOrder(User user) {
        // 支持对象属性
    }
}
```

### 批量锁

当参数是集合时，自动批量加锁：

```java
@Service
public class BatchService {
    
    @Lock("#userIds")  // 自动批量锁所有 userId
    public void batchUpdate(List<String> userIds) {
        // 对所有 userIds 加锁，确保原子性
    }
}
```

### 锁属性

```java
public class Example {
    @Lock(
        value = "#orderId",
        waitTime = 5000,    // 等待时间（毫秒）
        leaseTime = 30000   // 持有时间（毫秒）
    )
    public OrderDTO processOrder(String orderId) { }
}
```

---

## Pub/Sub 消息

### 定义消息处理器

```java
@Component
@RedisListener(channel = "order-events")
public class OrderEventHandler implements MessageDelegate {
    
    @Override
    public void handleMessage(String message) {
        // 处理消息
        OrderEvent event = Jsons.parse(message, OrderEvent.class);
        processEvent(event);
    }
}
```

### 发送消息

```java
@Service
public class OrderService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    public void publishOrderCreated(OrderDTO order) {
        redisTemplate.convertAndSend("order-events", Jsons.stringify(order));
    }
}
```

---

## 类型安全操作（TypeRedisTemplate）

### 构建类型安全的 RedisTemplate

```java
@Service
public class CacheService {
    
    @Autowired
    private TypeRedisTemplate typeRedisTemplate;
    
    public void cacheUser(UserDTO user) {
        RedisTemplate<String, UserDTO> template = typeRedisTemplate
            .builder()
            .keyClass(String.class)
            .valueClass(UserDTO.class)
            .build();
        
        template.opsForValue().set("user:" + user.getId(), user);
    }
}
```

---

## 核心类说明

| 类名 | 说明 |
|------|------|
| `RedisConfig` | Redis 自动配置，StringRedisTemplate + Jackson |
| `TypeRedisTemplate` | 类型安全的 RedisTemplate 构建器 |
| `RedissonConfig` | Redisson 客户端配置，支持单节点和集群 |
| `Lock` | 分布式锁注解 |
| `LockAspect` | 锁切面实现，支持批量锁 |
| `RedisPubsubConfig` | Pub/Sub 自动配置 |
| `MessageDelegate` | 消息处理接口 |

---

## 使用场景

### 场景1：防止重复提交

```java
@Service
public class PaymentService {
    
    @Lock("'payment:' + #request.orderId")
    public PaymentResult pay(PaymentRequest request) {
        // 同一订单只能支付一次
    }
}
```

### 场景2：库存扣减

```java
@Service
public class InventoryService {
    
    @Lock("'stock:' + #productId")
    public void deductStock(String productId, int quantity) {
        // 防止超卖
        int stock = getStock(productId);
        if (stock >= quantity) {
            updateStock(productId, stock - quantity);
        }
    }
}
```

### 场景3：事件通知

```java
// 订单创建后通知其他服务
@Service
public class OrderService {
    
    @Lock("#orderId")
    public OrderDTO createOrder(String orderId) {
        OrderDTO order = save(orderId);
        
        // 发布事件
        redisTemplate.convertAndSend("order-created", order);
        
        return order;
    }
}
```

---

## 配置属性

```properties
# Redis 基础配置
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=
spring.redis.database=0

# 集群配置
spring.redis.cluster.nodes=redis1:6379,redis2:6379,redis3:6379

# 分布式锁配置
org.source.spring.enabled.lock=true
```

---

## 相关资源

- **GitHub 仓库**: [spring-extension-starter](https://github.com/Dao1230source/spring-extension-starter)
- **示例代码**: [Demo Project](https://github.com/Dao1230source/demo)
- **问题反馈**: [GitHub Issues](https://github.com/Dao1230source/spring-extension-starter/issues)

---

## 标签（Tags）

`redis` `distributed-lock` `pub-sub` `redisson` `type-safe-template` `batch-lock` `json-serialization` `spring-boot-starter` `分布式锁` `发布订阅` `类型安全`