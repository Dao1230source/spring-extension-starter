# Spring Extension Stream - 消息流扩展组件

> **关键词（Keywords）**: Spring Cloud Stream, REST Messaging, Message Bridge, HTTP to Stream, Producer, Consumer, Binder, 消息流, REST消息, HTTP转消息, 生产者消费者

[![Maven Central](https://img.shields.io/maven-central/v/io.github.dao1230source/spring-extension-starter-stream.svg)](https://central.sonatype.com/artifact/io.github.dao1230source/spring-extension-starter-stream)
[![Java](https://img.shields.io/badge/Java-21+-green.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)

## 概述

Spring Cloud Stream 的 REST 扩展，提供：

- **REST 消息桥接**：将 HTTP 请求作为消息处理
- **声明式生产者/消费者**：通过接口定义消息通道
- **动态配置**：运行时启用/禁用生产者和消费者
- **多系统支持**：支持多个独立的消息系统配置

**适用于 Webhook、事件驱动架构、混合 HTTP/消息系统场景**。

---

## 快速开始

### 1. 添加依赖

**Maven:**

```xml
<dependency>
    <groupId>io.github.dao1230source</groupId>
    <artifactId>spring-extension-starter-stream</artifactId>
    <version>0.0.12</version>
</dependency>
```

### 2. 启用 Stream 路由

```java
@SpringBootApplication
@EnableExtendedStreamRouter
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. 定义消息处理器

```java
@Component
public class OrderMessageHandler implements ConsumerProcessor<OrderEvent> {
    
    @Override
    public void process(OrderEvent event) {
        // 处理订单事件
        System.out.println("Received order: " + event.getOrderId());
    }
}
```

### 4. 配置消息通道

```properties
# 系统配置
spring.cloud.stream.systems.order-system.producers.orderCreated.isEnable=true
spring.cloud.stream.systems.order-system.consumers.orderHandler.isEnable=true
spring.cloud.stream.systems.order-system.consumers.orderHandler.path=/webhook/orders
```

---

## REST 消息桥接

### 作为消费者接收 HTTP 请求

```properties
# 配置 REST 消费者
spring.cloud.stream.systems.webhook-system.consumers.restConsumer.isEnable=true
spring.cloud.stream.systems.webhook-system.consumers.restConsumer.path=/api/webhook
```

```java
@Component
public class WebhookHandler implements ConsumerProcessor<WebhookPayload> {
    
    @Override
    public void process(WebhookPayload payload) {
        // 处理 Webhook 请求
        // HTTP POST /api/webhook 会自动路由到此处理器
    }
}
```

### 作为生产者发送消息

```java
@Service
public class EventPublisher {
    
    @Autowired
    private ProducerProcessor<EventPayload> eventProducer;
    
    public void publishEvent(EventPayload event) {
        eventProducer.process(event);  // 发送到消息通道
    }
}
```

---

## 核心类说明

| 类名 | 说明 |
|------|------|
| `EnableExtendedStreamRouter` | 启用 Stream 路由功能 |
| `StreamBinder` | 核心绑定器，管理消息通道 |
| `StreamBinderProperties` | 绑定属性配置 |
| `ProducerProcessor<T>` | 生产者接口 |
| `ConsumerProcessor<T>` | 消费者接口 |
| `RestBinderConfiguration` | REST 绑定配置 |
| `RestProducer` | REST 生产者实现 |
| `RestListener` | REST 消费者监听器 |

---

## 使用场景

### 场景1：Webhook 接收

```java
// 接收第三方 Webhook
@Component
public class GithubWebhookHandler implements ConsumerProcessor<GithubEvent> {
    
    @Override
    public void process(GithubEvent event) {
        // 处理 GitHub 推送事件
    }
}
```

```properties
spring.cloud.stream.systems.github.consumers.webhook.isEnable=true
spring.cloud.stream.systems.github.consumers.webhook.path=/webhook/github
```

### 场景2：事件驱动架构

```java
// 订单服务发布事件
@Service
public class OrderService {
    @Autowired
    private ProducerProcessor<OrderEvent> orderProducer;
    
    public void createOrder(OrderForm form) {
        Order order = saveOrder(form);
        
        // 发布事件到消息流
        orderProducer.process(new OrderEvent("created", order));
    }
}

    // 库存服务消费事件
    @Component
public class InventoryHandler implements ConsumerProcessor<OrderEvent> {
    @Override
    public void process(OrderEvent event) {
        // 扣减库存
    }
}
```

### 场景3：混合架构

```
HTTP Request → REST Consumer → Message Stream → Multiple Consumers
```

---

## 配置属性

```properties
# 系统级别配置
spring.cloud.stream.systems.{systemName}.producers.{producerName}.isEnable=true
spring.cloud.stream.systems.{systemName}.consumers.{consumerName}.isEnable=true
spring.cloud.stream.systems.{systemName}.consumers.{consumerName}.path=/api/messages
```

---

## 与原生 Spring Cloud Stream 对比

| 特性 | 本模块 | 原生 |
|------|--------|------|
| REST 消息桥接 | ✅ 内置 | ❌ 不支持 |
| 动态启用/禁用 | ✅ 运行时 | ⚠️ 启动时 |
| 多系统配置 | ✅ 独立配置 | ⚠️ 统一配置 |
| 接口驱动 | ✅ 简洁 | ⚠️ 注解较多 |

---

## 相关资源

- **GitHub 仓库**: [spring-extension-starter](https://github.com/Dao1230source/spring-extension-starter)
- **示例代码**: [Demo Project](https://github.com/Dao1230source/demo)
- **问题反馈**: [GitHub Issues](https://github.com/Dao1230source/spring-extension-starter/issues)

---

## 标签（Tags）

`spring-cloud-stream` `rest-messaging` `message-bridge` `http-to-stream` `producer-consumer` `binder` `webhook` `spring-boot-starter` `消息流` `REST消息`