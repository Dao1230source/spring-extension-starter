# Spring Extension IO - 输入输出封装组件

> **关键词（Keywords）**: Input Output Wrapper, API Response, Request Validation, Jakarta Validation, Enum Validation,
> Validation Groups, Distributed Tracing, 请求响应封装, 参数校验, 枚举校验, 分布式追踪

[![Maven Central](https://img.shields.io/maven-central/v/io.github.dao1230source/spring-extension-starter-io.svg)](https://central.sonatype.com/artifact/io.github.dao1230source/spring-extension-starter-io)
[![Java](https://img.shields.io/badge/Java-21+-green.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)

## 概述

标准化的 Spring Boot 输入输出处理组件，提供：

- **统一请求封装（Input<T>）**：带验证支持的请求数据包装器
- **统一响应封装（Output<T>）**：标准化的 API 响应格式
- **增强的校验能力**：枚举值存在性校验、操作分组校验
- **分布式追踪集成**：自动注入 TraceId 到响应中

**构建一致的 API 契约**，简化 Controller 层代码，提升团队协作效率。

---

## 快速开始

### 1. 添加依赖

**Maven:**

```xml

<dependency>
    <groupId>io.github.dao1230source</groupId>
    <artifactId>spring-extension-starter-io</artifactId>
    <version>0.0.12</version>
</dependency>
```

### 2. 统一响应格式

```java

@RestController
public class UserController {

    @GetMapping("/users/{id}")
    public Output<UserDTO> getUser(@PathVariable String id) {
        UserDTO user = userService.findById(id);
        return Output.success(user);
    }

    @PostMapping("/users")
    public Output<UserDTO> createUser(@RequestBody @Valid Input<UserForm> input) {
        UserDTO user = userService.create(input.getData());
        return Output.success(user);
    }
}
```

---

## 核心类详解

### Input<T> - 请求封装

用于封装请求数据，支持 Jakarta 验证：

```java

@Data
public class CreateUserRequest {
    @Input  // 包装为 Input<CreateUserRequest>
    private CreateUserRequest data;
}

// 在 Controller 中使用
@PostMapping("/users")
public Output<UserDTO> create(@RequestBody @Valid Input<UserForm> input) {
    UserForm form = input.getData();  // 获取实际数据
    // ...
}
```

### Output<T> - 响应封装

统一的 API 响应格式：

```java
public class Output<T> {
    private String code;        // 状态码
    private String message;     // 消息
    private T data;             // 数据
    private String traceId;     // 追踪ID（自动注入）
    private Long timestamp;     // 时间戳
}

// 创建成功响应
Output<UserDTO> output = Output.success(user);

// 创建失败响应
Output<Void> output = Output.fail("USER_NOT_FOUND", "用户不存在");
```

**JSON 响应示例：**

```json
{
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {
    "id": "123",
    "name": "张三"
  },
  "traceId": "abc123def456",
  "timestamp": 1709251200000
}
```

---

## 增强校验

### 1. 枚举存在性校验（@EnumExists）

验证字段值是否存在于指定枚举中：

```java
public class OrderForm {

    @EnumExists(enumClass = OrderStatus.class, method = "getCode")
    private String status;  // 必须是 OrderStatus 枚举中的某个 code 值
}

// 枚举定义
public enum OrderStatus {
    PENDING("pending"),
    PROCESSING("processing"),
    COMPLETED("completed");

    private final String code;
}
```

### 2. 操作分组校验

按 CRUD 操作分组校验：

```java
public class UserForm {

    @NotNull(groups = Update.class)  // 更新时必填
    private String id;

    @NotNull(groups = Add.class)    // 新增时必填
    private String name;

    @NotNull(groups = {Add.class, Update.class})
    private String email;
}

// Controller 中使用
@PostMapping
public Output<UserDTO> create(@RequestBody @Validated(Add.class) Input<UserForm> input) {
}

@PutMapping("/{id}")
public Output<UserDTO> update(@RequestBody @Validated(Update.class) Input<UserForm> input) {
}
```

**内置校验分组：**

| 分组       | 说明   |
|----------|------|
| `Add`    | 新增操作 |
| `Update` | 更新操作 |
| `Delete` | 删除操作 |
| `Select` | 查询操作 |

---

## 分布式追踪集成

`Output` 自动从 `TraceContext` 获取并注入 TraceId：

```java
public class Example {
    @GetMapping("/orders/{id}")
    public Output<OrderDTO> getOrder(@PathVariable String id) {
        OrderDTO order = orderService.findById(id);
        return Output.success(order);
        // 响应中自动包含 traceId
    }
}
```

---

## 使用场景

### 场景1：标准 CRUD API

```java

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    public Output<UserDTO> getById(@PathVariable String id) {
        return Output.success(userService.findById(id));
    }

    @PostMapping
    public Output<UserDTO> create(
            @RequestBody @Validated(Add.class) Input<UserForm> input
    ) {
        return Output.success(userService.create(input.getData()));
    }

    @PutMapping("/{id}")
    public Output<UserDTO> update(
            @PathVariable String id,
            @RequestBody @Validated(Update.class) Input<UserForm> input
    ) {
        return Output.success(userService.update(id, input.getData()));
    }

    @DeleteMapping("/{id}")
    public Output<Void> delete(@PathVariable String id) {
        userService.delete(id);
        return Output.success();
    }
}
```

### 场景2：枚举字段校验

```java

@Data
public class OrderQuery {

    @EnumExists(enumClass = OrderStatus.class, method = "getCode")
    private String status;

    @EnumExists(enumClass = PaymentMethod.class, method = "getCode")
    private String paymentMethod;
}
```

### 场景3：异常统一处理

```java

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public Output<Void> handleBaseException(BaseException e) {
        return Output.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Output<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return Output.fail("VALIDATION_ERROR", message);
    }
}
```

---

## 相关资源

- **GitHub 仓库**: [spring-extension-starter](https://github.com/Dao1230source/spring-extension-starter)
- **示例代码**: [Demo Project](https://github.com/Dao1230source/demo)
- **问题反馈**: [GitHub Issues](https://github.com/Dao1230source/spring-extension-starter/issues)

---

## 标签（Tags）

`input-output` `api-response` `request-validation` `jakarta-validation` `enum-validation` `validation-groups`
`distributed-tracing` `spring-boot-starter` `请求响应` `参数校验` `枚举校验`