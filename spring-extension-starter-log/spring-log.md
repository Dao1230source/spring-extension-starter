# Spring Extension Log - 方法执行日志组件

> **关键词（Keywords）**: Method Logging, AOP Logging, Database Change Log, Audit Log, SpEL Expression, DataSource Proxy, 方法日志, 数据库变更日志, 审计日志, 操作日志

[![Maven Central](https://img.shields.io/maven-central/v/io.github.dao1230source/spring-extension-starter-log.svg)](https://central.sonatype.com/artifact/io.github.dao1230source/spring-extension-starter-log)
[![Java](https://img.shields.io/badge/Java-21+-green.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)

## 概述

基于 AOP 的方法执行日志和数据库变更日志组件，提供：

- **方法执行日志（@Log）**：记录方法入参、返回值、执行时间
- **数据库变更日志（@DataSourceLog）**：自动捕获 SQL 变更并记录
- **SpEL 表达式支持**：动态生成日志内容
- **自定义处理器**：支持将日志持久化到数据库或其他存储

**适用于审计、合规追踪、运营监控等场景**。

---

## 快速开始

### 1. 添加依赖

**Maven:**

```xml
<dependency>
    <groupId>io.github.dao1230source</groupId>
    <artifactId>spring-extension-starter-log</artifactId>
    <version>0.0.12</version>
</dependency>
```

### 2. 启用日志扩展

```java
@SpringBootApplication
@EnableExtendedLog
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3. 使用方法日志

```java
@Service
public class UserService {
    
    @Log("'用户查询: id=' + #id")
    public UserDTO findById(String id) {
        return userRepository.findById(id);
    }
    
    @Log(value = "'创建用户: ' + #user.name", recordResult = true)
    public UserDTO create(UserForm user) {
        return userRepository.save(user);
    }
}
```

---

## 核心注解

### @Log - 方法执行日志

记录方法执行的详细信息：

```java
public class Example {
    @Log(
        value = "'操作: ' + #action",  // SpEL 表达式
        recordArgs = true,              // 记录入参
        recordResult = true,            // 记录返回值
        recordTime = true               // 记录执行时间
    )
    public void performAction(String action) { }
}
```

**属性说明：**

| 属性 | 类型 | 说明 |
|------|------|------|
| `value` | String | SpEL 表达式，生成日志内容 |
| `recordArgs` | boolean | 是否记录方法入参 |
| `recordResult` | boolean | 是否记录返回值 |
| `recordTime` | boolean | 是否记录执行时间 |

### @DataSourceLog - 数据库变更日志

自动捕获并记录数据库变更：

```java
public class Example {
    @DataSourceLog(
        tables = {"users", "orders"},   // 监控的表
        excludes = {"password", "salt"} // 排除的字段
    )
    public void updateUser(UserForm form) {
        userRepository.update(form);
        // 自动记录 SQL 变更
}
}
```

---

## 自定义日志处理器

### 默认实现

默认使用 `DefaultLogDataProcessor`，仅输出到 SLF4J 日志：

```java
@Slf4j
public class DefaultLogDataProcessor implements LogDataProcessor {
    @Override
    public void process(LogData logData) {
        log.info("Log data: {}", logData);
    }
}
```

### 自定义持久化实现

```java
@Component
public class DatabaseLogDataProcessor implements LogDataProcessor {
    
    @Autowired
    private LogRepository logRepository;
    
    @Override
    public void process(LogData logData) {
        LogEntity entity = new LogEntity();
        entity.setContent(logData.getContent());
        entity.setTimestamp(logData.getTimestamp());
        entity.setTraceId(logData.getTraceId());
        logRepository.save(entity);
    }
}
```

---

## 数据库变更日志

### 配置数据源代理

日志模块自动代理 DataSource，拦截 SQL 执行：

```java
public class Example {
    public void example() {
        // 自动配置，无需手动干预
        // 通过 DataSourceProxyBeanPostProcessor 实现
    }
}
```

### 变更日志数据结构

```java
public class DataSourceLogData {
    private String tableName;      // 表名
    private String operation;      // 操作类型（INSERT/UPDATE/DELETE）
    private String oldValue;       // 旧值（JSON）
    private String newValue;       // 新值（JSON）
    private String traceId;        // 追踪ID
    private Long timestamp;        // 时间戳
}
```

---

## 使用场景

### 场景1：关键操作审计

```java
@Service
public class PaymentService {
    
    @Log("'支付请求: orderId=' + #orderId + ', amount=' + #amount")
    @DataSourceLog(tables = {"payments", "orders"})
    public PaymentResult pay(String orderId, BigDecimal amount) {
        // 支付逻辑
        // 自动记录操作日志和数据库变更
    }
}
```

### 场景2：敏感操作追踪

```java
@Service
public class SecurityService {
    
    @Log(value = "'修改密码: userId=' + #userId", recordArgs = false)
    public void changePassword(String userId, String oldPassword, String newPassword) {
        // 不记录密码参数
    }
}
```

### 场景3：批量日志处理

```java
@Service
public class BatchService {
    
    @Log(value = "'批量导入: count=' + #items.size()", recordTime = true)
    public void importItems(List<Item> items) {
        // 批量导入逻辑
    }
}
```

---

## 配置属性

```properties
# 启用日志功能（默认 true）
org.source.spring.log.enabled=true

# 日志级别
logging.level.org.source.spring.log=DEBUG
```

---

## 与其他方案对比

| 特性 | 本模块 | Spring AOP 手动实现 | Logback |
|------|--------|---------------------|---------|
| 方法日志 | ✅ 注解驱动 | ⚠️ 需编写切面 | ❌ 不支持 |
| 数据库变更 | ✅ 自动捕获 | ❌ 需手动实现 | ❌ 不支持 |
| SpEL 支持 | ✅ 动态内容 | ⚠️ 需手动解析 | ❌ 不支持 |
| 自定义存储 | ✅ 可扩展 | ✅ 可扩展 | ⚠️ 有限 |

---

## 相关资源

- **GitHub 仓库**: [spring-extension-starter](https://github.com/Dao1230source/spring-extension-starter)
- **示例代码**: [Demo Project](https://github.com/Dao1230source/demo)
- **问题反馈**: [GitHub Issues](https://github.com/Dao1230source/spring-extension-starter/issues)

---

## 标签（Tags）

`method-logging` `aop-logging` `database-change-log` `audit-log` `spel-expression` `datasource-proxy` `spring-boot-starter` `方法日志` `数据库变更` `审计日志` `操作日志`