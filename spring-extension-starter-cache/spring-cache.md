# Spring Extension Cache - Spring Cache 增强扩展

> **关键词（Keywords）**: Spring Cache, Redis Cache, JVM Cache, Two-Level Cache, Batch Cache, Multi-Get Cache, Partial Cache, Distributed Cache, Spring Boot Cache Extension, 缓存增强, 批量缓存, 二级缓存, 分布式缓存

[![Maven Central](https://img.shields.io/maven-central/v/io.github.dao1230source/spring-extension-starter-cache.svg)](https://central.sonatype.com/artifact/io.github.dao1230source/spring-extension-starter-cache)
[![Java](https://img.shields.io/badge/Java-21+-green.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)

## 概述

基于 Spring Cache 框架的增强扩展，提供以下核心功能：

- **批量缓存获取（Batch Cache / Multi-Get）**：支持 Collection 入参的批量方法，一次性获取多个缓存项，减少网络往返
- **二级缓存架构（Two-Level Cache / L1+L2 Cache）**：支持 JVM 本地缓存 + Redis 远程缓存的多级缓存策略
- **智能缓存同步（Distributed Cache Sync）**：基于 Redis Pub/Sub 的分布式缓存失效通知机制
- **灵活的部分缓存处理（Partial Cache Strategy）**：当部分数据缺失时的多种处理策略（TRUST/DISTRUST/PARTIAL_TRUST）
- **Redis 分片缓存（Redis Sharded Cache）**：使用 HSET 分片存储海量数据，避免 Redis key 过多影响性能
- **参数化类型支持（Generic Type Support）**：支持 Generic 类型的缓存存储和反序列化
- [架构UML图](Spring_Extension_Cache_Architecture.png)

**支持的 Spring Cache 原生注解**：`@Cacheable`、`@Caching`、`@CacheEvict`、`@CachePut` 等都可以正常使用

---

## 适用场景

| 场景 | 推荐配置 | 说明 |
|------|----------|------|
| 热点数据高频查询 | 二级缓存（JVM+Redis） | 如商品详情、用户信息 |
| 批量数据查询 | 批量缓存 + PARTIAL_TRUST | 如订单列表、用户列表 |
| 分布式环境缓存同步 | JVM缓存 + Redis Pub/Sub | 多实例部署场景 |
| 临时数据缓存 | 仅JVM缓存 | 无需持久化的临时数据 |
| 冷数据长期存储 | 仅Redis缓存 | 更新频率低的数据 |
| 海量数据缓存 | Redis 分片缓存（FIXED_SHARD/FIXED_SIZE） | 百万级数据，避免 key 过多 |

---

## 快速开始

### 0. 添加依赖

**Maven:**

```xml
<dependency>
    <groupId>io.github.dao1230source</groupId>
    <artifactId>spring-extension-starter-cache</artifactId>
    <version>0.0.12</version>
</dependency>
```

**Gradle:**

```groovy
implementation 'io.github.dao1230source:spring-extension-starter-cache:0.0.12'
```

**前置依赖:**
- Spring Boot 3.x
- Spring Data Redis（如使用 Redis 缓存）
- JDK 21+

### 1. 启用扩展缓存

在Application类上添加注解：

```java
@SpringBootApplication
@EnableExtendedCache
public class YourApplication {
    static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

### 2. 配置TTL（可选）

在`application.properties`或`application.yml`中配置默认过期时间：

```properties
# Redis缓存默认过期时间（秒）
org.source.spring.cache.redis-ttl=300
# JVM缓存默认过期时间（秒）
org.source.spring.cache.jvm-ttl=300
```

### 3. 在方法上使用@ConfigureCache注解

```java
public class Example {

    @ConfigureCache(cacheNames = "users", key = "#id")
    public User getUserById(String id) {
        return userService.fetchFromDb(id);
}
}
```

---

## 核心注解详解

### @ConfigureCache - 主注解

标注在方法上，声明该方法需要缓存。继承自`@Cacheable`，支持所有原生属性。

#### 基础属性（继承自@Cacheable）

| 属性                | 类型       | 说明                   |
|-------------------|----------|----------------------|
| `cacheNames()`    | String[] | 缓存名称，必填              |
| `key()`           | String   | SpEL表达式，定义缓存key      |
| `cacheResolver()` | String   | CacheResolver bean名称 |
| `condition()`     | String   | SpEL条件表达式，满足时才缓存     |

#### 扩展属性

| 属性                       | 类型                       | 说明                                   |
|--------------------------|--------------------------|--------------------------------------|
| `cacheKeySpEl()`         | String                   | **批量缓存专用** - 从返回值中提取单条数据的key的SpEL表达式 |
| `returnType()`           | ReturnTypeEnum           | 返回值类型，支持LIST、MAP、SET、RAW、AUTO        |
| `partialCacheStrategy()` | PartialCacheStrategyEnum | 部分数据缺失时的处理策略                         |
| `cacheInRedis()`         | @CacheInRedis            | Redis缓存配置                            |
| `cacheInJvm()`           | @CacheInJvm              | JVM缓存配置                              |

#### cacheKeySpEl() 详解

当方法返回Collection或Map时，需要指定`cacheKeySpEl()`来定义如何从单条数据中提取缓存key。

使用`#R`占位符表示返回值中的单条数据：

```java
public class Example {
    // 返回List<Student>，key为student.id
    @ConfigureCache(
            cacheNames = "students",
            key = "#ids",
            cacheKeySpEl = "#R.id"  // 从Student对象中提取id作为缓存key
    )
    public List<Student> getStudentsByIds(List<String> ids) {
}

    // 返回Set<User>，key为user.email
    @ConfigureCache(
            cacheNames = "users",
            key = "#emails",
            cacheKeySpEl = "#R.email"
    )
    public Set<User> getUsersByEmails(Collection<String> emails) {
}
}
```

#### returnType() 详解

自动类型判断机制：

| 值          | 适用场景                                          |
|------------|-----------------------------------------------|
| `AUTO`（默认） | 框架自动判断，推荐使用                                   |
| `LIST`     | 返回值是List<E>                                   |
| `SET`      | 返回值是Set<E>                                    |
| `MAP`      | 返回值是Map<K,V>                                  |
| `RAW`      | 返回值本身是Collection/Map的包装类，如`List<StudentView>` |

---

### @CacheInRedis - Redis缓存配置

```java
public class AnnotationExample {
    @interface CacheInRedis {
        // 是否启用Redis缓存，默认true
        boolean enable() default true;

        // 过期时间（秒），默认从配置文件读取
        long ttl() default CacheConstant.FROM_CONFIG;

        // 缓存值的Java类型（用于反序列化）
        Class<?>[] valueClasses() default {};

        // 分片策略，默认不分片
        ShardStrategyEnum shardStrategy() default ShardStrategyEnum.NONE;

        // 分片参数（含义随策略不同而不同）
        int shardValue() default 16;
    }
}
```

#### shardStrategy() 分片策略说明

当业务 key 数量很大时（百万级），使用 HSET 分片存储可以避免 Redis key 过多影响性能。

| 策略 | 说明 | shardValue 含义 | 分片索引计算 |
|------|------|-----------------|--------------|
| `NONE` | 不分片（默认） | - | - |
| `FIXED_SHARD` | 按 hashCode 分片 | 分片总数量 | `shardIndex = hashCode(key) % shardValue` |
| `FIXED_SIZE` | 按数值范围分片 | 每个分片的数据量 | `shardIndex = (key / shardValue) + 1` |

**FIXED_SHARD 示例**（适用于随机分布的 key）：
```java
public class Example {
    // 分8个HSET存储，每个HSET存储约N/8条数据
    @ConfigureCache(
            cacheNames = "products",
            key = "#ids",
            cacheKeySpEl = "#R.id",
            cacheInRedis = @CacheInRedis(
                    shardStrategy = ShardStrategyEnum.FIXED_SHARD,
                    shardValue = 8,
                    valueClasses = {Product.class}
            )
    )
    public List<Product> getProductsByIds(Collection<String> ids) {}
}
```

**FIXED_SIZE 示例**（适用于有序自增主键）：
```java
public class Example {
    // 按ID范围分片：ID 1-49999 存入 shard:1，ID 50000-99999 存入 shard:2
    @ConfigureCache(
            cacheNames = "orders",
            key = "#orderIds",
            cacheKeySpEl = "#R.id",
            cacheInRedis = @CacheInRedis(
                    shardStrategy = ShardStrategyEnum.FIXED_SIZE,
                    shardValue = 50000,
                    valueClasses = {Order.class}
            )
    )
    public List<Order> getOrdersByIds(Collection<Long> orderIds) {}
}
```

**注意**：FIXED_SIZE 策略要求 key 必须是 Integer 或 Long 类型。

#### valueClasses() 说明

指定Redis中存储的值类型，用于JSON反序列化：

- **单条缓存**：valueClasses不需要指定，自动使用方法返回值类型
- **批量缓存**：指定容器内元素的类型
    - `Map<K,V>`返回值：valueClasses = {V.class}
    - `List<E>`返回值：valueClasses = {E.class}
    - `Set<E>`返回值：valueClasses = {E.class}

```java
public class Example {
    // 返回Map<String, Student>
    @ConfigureCache(
            cacheNames = "students",
            key = "#ids",
            cacheInRedis = @CacheInRedis(valueClasses = {Student.class})
    )
    public Map<String, Student> getStudentMap(List<String> ids) {
}
}
```

---

### @CacheInJvm - JVM本地缓存配置

```java
public class AnnotationExample {
    @interface CacheInJvm {
        // 是否启用JVM缓存，默认false
        boolean enable() default false;

        // 过期时间（秒），默认从配置文件读取
        long ttl() default CacheConstant.FROM_CONFIG;

        // 最大缓存条目数，-1表示不限制
        long jvmMaxSize() default CacheConstant.NO_LIMIT;

        // 缓存key的类型
        Class<?> keyClass() default String.class;
    }
}
```

#### keyClass() 说明

指定JVM缓存中key的类型：

- **单条缓存**：key = @ConfigureCache.key()的值
- **批量缓存**：
    - 返回`Map<K,V>`：key = K
    - 返回`List<E>/Set<E>`：key = 通过cacheKeySpEl提取的E值

```java
public class Example {
    // JVM中的key为String类型（学生ID）
    @ConfigureCache(
            cacheNames = "students",
            key = "#ids",
            cacheKeySpEl = "#R.id",
            cacheInJvm = @CacheInJvm(enable = true, jvmMaxSize = 10000)
    )
    public List<Student> getStudents(List<String> ids) {
}
}
```

---

## 使用场景详解

### 场景1：单条缓存（简单使用）

```java
public class Example {

    @ConfigureCache(cacheNames = "user", key = "#id")
    public User getUserById(String id) {
        return userService.findById(id);
}
}
```

- 自动缓存到Redis（如已配置）
- 每次调用时先查询缓存，缓存命中返回

### 场景2：批量缓存 - 返回List

当需要批量查询多个资源时：

```java
public class Example {

    @ConfigureCache(
            cacheNames = "students",
            key = "#ids",
            cacheKeySpEl = "#R.id",  // 关键：指定如何从Student提取key
            cacheInRedis = @CacheInRedis(valueClasses = {Student.class})
    )
    public List<Student> getStudentsByIds(Collection<String> ids) {
        // 框架会自动：
        // 1. 批量查询缓存（每个id一个key）
        // 2. 对缺失的id调用此方法
        // 3. 缓存新获取的数据
        // 4. 合并返回完整结果
        return studentService.findByIds(ids);
}
}
```

### 场景3：批量缓存 - 返回Map

当返回值是Map时，key自动从Map中提取：

```java
public class Example {

    @ConfigureCache(
            cacheNames = "studentMap",
            key = "#ids",
            cacheInRedis = @CacheInRedis(valueClasses = {Student.class})
    )
    public Map<String, Student> getStudentMapByIds(Collection<String> ids) {
        // Map的key会自动作为缓存key
        return studentService.findMapByIds(ids);
}
}
```

### 场景4：二级缓存架构

同时使用JVM和Redis缓存，提高查询性能：

```java
public class Example {

    @ConfigureCache(
            cacheNames = "hotUsers",
            key = "#id",
            cacheInRedis = @CacheInRedis(ttl = 3600),  // Redis缓存1小时
            cacheInJvm = @CacheInJvm(enable = true, ttl = 300, jvmMaxSize = 5000)  // JVM缓存5分钟
    )
    public User getHotUser(String id) {
        return userService.findById(id);
}
}
```

查询流程：

1. 先查询JVM缓存（最快）
2. JVM缓存未命中 → 查询Redis
3. Redis未命中 → 执行方法获取数据
4. 将数据同时存入JVM和Redis

### 场景5：部分缓存策略 - TRUST

信任所有缓存，即使某些key未找到：

```java
public class Example {

    @ConfigureCache(
            cacheNames = "students",
            key = "#ids",
            cacheKeySpEl = "#R.id",
            partialCacheStrategy = PartialCacheStrategyEnum.TRUST
    )
    public List<Student> getStudents(List<String> ids) {
}
}
```

- 缓存命中的数据直接返回
- 缺失的key**不会再次查询**，只返回缓存中存在的数据

### 场景6：部分缓存策略 - DISTRUST（默认）

不信任缓存，任何缺失都重新调用方法：

```java
public class Example {

    @ConfigureCache(
            cacheNames = "students",
            key = "#ids",
            cacheKeySpEl = "#R.id",
            partialCacheStrategy = PartialCacheStrategyEnum.DISTRUST
    )
    public List<Student> getStudents(Collection<String> ids) {
}
}
```

- 如果部分key缺失，会用缺失的key重新调用此方法
- 适合对一致性要求高的场景

### 场景7：部分缓存策略 - PARTIAL_TRUST

智能处理：缓存命中+对缺失数据重新查询：

```java
public class Example {

    @ConfigureCache(
            cacheNames = "students",
            key = "#ids",  // 必须指向第一个参数或为空
            cacheKeySpEl = "#R.id",
            partialCacheStrategy = PartialCacheStrategyEnum.PARTIAL_TRUST
    )
    public List<Student> getStudents(Collection<String> ids) {
}
}
```

**使用限制**：

- 方法必须有且仅有一个参数
- 入参必须是可变Collection（不能是`List.of()`创建的不可变列表）
- `key()`无值或指向第一个参数

**优势**：避免不必要的完整重查，性能最优

### 场景8：JVM缓存参数化类型

缓存复杂的泛型对象：

```java
public class Example {

    @ConfigureCache(
            cacheNames = "classCache",
            key = "#className",
            returnType = ReturnTypeEnum.RAW,  // 显式声明返回类型是容器
            cacheInRedis = @CacheInRedis(enable = false),
            cacheInJvm = @CacheInJvm(enable = true)
    )
    public List<StudentView> getStudentsByClass(String className) {
}
}
```

### 场景9：Redis 分片缓存 - FIXED_SHARD 策略

当缓存数据量很大时（百万级），使用分片存储避免 Redis key 过多：

```java
public class Example {

    /**
     * 商品缓存 - 使用 FIXED_SHARD 策略
     * - shardValue = 16：分16个HSET存储
     * - Redis key格式：products:shard:0 ~ products:shard:15
     * - 每个 key 按 hashCode 分配到对应分片
     */
    @ConfigureCache(
            cacheNames = "products",
            key = "#ids",
            cacheKeySpEl = "#R.id",
            partialCacheStrategy = PartialCacheStrategyEnum.PARTIAL_TRUST,
            cacheInRedis = @CacheInRedis(
                    shardStrategy = ShardStrategyEnum.FIXED_SHARD,
                    shardValue = 16,
                    valueClasses = {Product.class}
            )
    )
    public List<Product> getProductsByIds(Collection<String> ids) {
        return productRepository.findByIds(ids);
}
}
```

**FIXED_SHARD Redis 结构**：
```
Key: products:shard:0 (HSET)
  field: "product_1" → serialized(Product)
  field: "product_8" → serialized(Product)
Key: products:shard:1 (HSET)
  field: "product_2" → serialized(Product)
...
Key: products:shard:15 (HSET)
```

### 场景10：Redis 分片缓存 - FIXED_SIZE 策略

适用于有序自增主键场景（如数据库 ID）：

```java
public class Example {

    /**
     * 订单缓存 - 使用 FIXED_SIZE 策略
     * - shardValue = 100000：每10万条数据一个分片
     * - 分片计算：shardIndex = (orderId / 100000) + 1
     * - ID 1-99999 → shard:1，ID 100000-199999 → shard:2
     */
    @ConfigureCache(
            cacheNames = "orders",
            key = "#orderIds",
            cacheKeySpEl = "#R.id",
            partialCacheStrategy = PartialCacheStrategyEnum.PARTIAL_TRUST,
            cacheInRedis = @CacheInRedis(
                    shardStrategy = ShardStrategyEnum.FIXED_SIZE,
                    shardValue = 100000,
                    valueClasses = {Order.class}
            )
    )
    public List<Order> getOrdersByIds(Collection<Long> orderIds) {
        // 注意：orderIds 必须是 Long 类型
        return orderRepository.findByIds(orderIds);
}
}
```

**FIXED_SIZE Redis 结构**：
```
Key: orders:shard:1 (HSET)
  field: "1" → serialized(Order)
  ...
  field: "99999" → serialized(Order)
Key: orders:shard:2 (HSET)
  field: "100000" → serialized(Order)
  ...
  field: "199999" → serialized(Order)
```

**分片策略选择指南**：

| 场景 | 推荐策略 | 原因 |
|------|----------|------|
| 随机字符串 key（如 UUID） | FIXED_SHARD | hashCode 分布均匀 |
| 有序自增主键（如数据库 ID） | FIXED_SIZE | 按范围分片，查询效率高 |
| 混合类型 key | FIXED_SHARD | 兼容性好 |
| 需要范围查询 | FIXED_SIZE | 同一范围的数据在同一分片 |

### 场景11：二级缓存 + 分片存储

同时使用 JVM 缓存和 Redis 分片存储：

```java
public class Example {

    /**
     * 热点商品 - 二级缓存 + 分片
     * - JVM 缓存：5分钟过期，最多10000条
     * - Redis 分片：8个HSET，1小时过期
     */
    @ConfigureCache(
            cacheNames = "hotProducts",
            key = "#ids",
            cacheKeySpEl = "#R.id",
            partialCacheStrategy = PartialCacheStrategyEnum.PARTIAL_TRUST,
            cacheInRedis = @CacheInRedis(
                    ttl = 3600,
                    shardStrategy = ShardStrategyEnum.FIXED_SHARD,
                    shardValue = 8,
                    valueClasses = {Product.class}
            ),
            cacheInJvm = @CacheInJvm(enable = true, ttl = 300, jvmMaxSize = 10000)
    )
    public List<Product> getHotProducts(Collection<String> ids) {
        return productRepository.findByIds(ids);
}
}
```

**查询流程**：
1. 先查询 JVM 本地缓存
2. JVM 未命中 → 查询 Redis 分片 HSET
3. Redis 未命中 → 执行业务方法
4. 结果同时存入 JVM 和 Redis 分片

---

## 分布式场景 - 缓存失效同步

在分布式环境中，当一个实例更新缓存时，需要通知其他实例删除本地缓存。

框架通过Redis Pub/Sub自动实现：

```java
public class Example {
    // 更新操作，使用@CacheEvict删除缓存
    @CacheEvict(cacheNames = "students", key = "#id")
    public void updateStudent(String id, StudentForm form) {
        studentService.update(id, form);
        // 框架自动发布Pub/Sub消息到所有实例
}
}
```

**自动流程**：

1. A实例执行@CacheEvict
2. 框架发布消息到Redis Topic: `CONFIGURE_CACHE::JVM_CACHE_EVICT`
3. 所有实例（包括A）收到消息
4. 各实例删除对应的JVM本地缓存

---

## 调试与监控

### 启用调试日志

```properties
logging.level.org.source=debug
```

### 关键日志标记

| 日志内容                                                    | 含义            |
|---------------------------------------------------------|---------------|
| `ConfigureCache get from jvm,key`                       | 从JVM缓存命中      |
| `ConfigureCache get from redis, key`                    | 从Redis缓存命中    |
| `ConfigureCache put jvm`                                | 数据保存到JVM      |
| `ConfigureCache put redis`                              | 数据保存到Redis    |
| `ConfigureCache publish evict message, receive clients` | 发布缓存失效通知      |
| `ConfigureCache evict jvm via pubsub`                   | 接收到缓存失效通知     |
| `ConfigureCache invoke method again`                    | 部分缓存缺失，重新执行方法 |

### 示例：追踪批量查询

```bash
# 启用debug日志
logging.level.org.source=debug

# 执行批量查询
studentService.getStudentsByIds(["id1", "id2", "id3"])

# 预期日志（假设id1、id2命中，id3缺失）
# ConfigureCache get from jvm,key: students::id1 ✓
# ConfigureCache get from jvm,key: students::id2 ✓
# ConfigureCache get from jvm,key: students::id3 ✗
# ConfigureCache invoke method again, args: [id3]
# ConfigureCache put jvm: students::id3
# 返回三条完整数据
```

---

## 最佳实践

### 1. 选择合适的缓存级别

```java
public class Example {
    // 热点数据 → 二级缓存
    @ConfigureCache(
            cacheNames = "hotItems",
            key = "#id",
            cacheInJvm = @CacheInJvm(enable = true),
            cacheInRedis = @CacheInRedis(ttl = 3600)
    )
    public Item getHotItem(String id) {
}

    // 冷数据 → 仅Redis
    @ConfigureCache(
            cacheNames = "coldItems",
            key = "#id",
            cacheInRedis = @CacheInRedis(ttl = 86400)
    )
    public Item getColdItem(String id) {
}

    // 临时数据 → 仅JVM
    @ConfigureCache(
            cacheNames = "tempData",
            key = "#id",
            cacheInJvm = @CacheInJvm(enable = true, ttl = 60),
            cacheInRedis = @CacheInRedis(enable = false)
    )
    public TempData getTempData(String id) {
}
}
```

### 2. 批量查询合理配置

```java
public class Example {
    // ❌ 不好：过大的批量，容易导致单次查询缓存太多
    @ConfigureCache(cacheNames = "users", key = "#ids", cacheKeySpEl = "#R.id")
    public List<User> getUsersByIds(Collection<String> ids) {
}

    // ✅ 好：在Service层限制批量大小
    public List<User> getUsersByIds(Collection<String> ids) {
        if (ids.size() > 100) {
            // 分批处理
            return ids.stream()
                    .collect(Collectors.groupingBy(id -> {
                }, Collectors.toList()))
                    .values().stream()
                    .flatMap(batch -> getUsersBatch(batch).stream())
                    .collect(Collectors.toList());
    }
        return getUsersBatch(ids);
}

    @ConfigureCache(cacheNames = "users", key = "#ids", cacheKeySpEl = "#R.id")
    private List<User> getUsersBatch(Collection<String> ids) {
}
}
```

### 3. 部分缓存策略的选择

```java
public class Example {
    // DISTRUST（默认）：数据一致性最高，但可能重复查询
    @ConfigureCache(
            cacheNames = "orders",
            partialCacheStrategy = PartialCacheStrategyEnum.DISTRUST
    )
    public List<Order> getOrders(Collection<String> orderIds) {
}

    // PARTIAL_TRUST：性能最优，但有分布式事务风险
    @ConfigureCache(
            cacheNames = "products",
            partialCacheStrategy = PartialCacheStrategyEnum.PARTIAL_TRUST
    )
    public List<Product> getProducts(Collection<String> productIds) {
}

    // TRUST：适合允许短期不一致的场景（推荐使用少）
    @ConfigureCache(
            cacheNames = "stats",
            partialCacheStrategy = PartialCacheStrategyEnum.TRUST
    )
    public List<Stat> getStats(Collection<String> statIds) {
}
}
```

### 4. 明确指定valueClasses

```java
public class Example {
    // ❌ 不好：未指定，可能反序列化失败
    @ConfigureCache(cacheNames = "students", key = "#ids", cacheKeySpEl = "#R.id")
    public List<StudentView> getStudents(List<String> ids) {
}

    // ✅ 好：明确指定元素类型
    @ConfigureCache(
            cacheNames = "students",
            key = "#ids",
            cacheKeySpEl = "#R.id",
            cacheInRedis = @CacheInRedis(valueClasses = {StudentView.class})
    )
    public List<StudentView> getStudents(List<String> ids) {
}
}
```

### 5. 合理设置TTL

```properties
# 根据数据更新频率设置不同的TTL
org.source.spring.cache.redis-ttl=300      # 默认5分钟
org.source.spring.cache.jvm-ttl=60         # JVM缓存更短
```

```java
public class Example {
    // 或在注解中覆盖
    @ConfigureCache(
            cacheNames = "realtime",
            cacheInRedis = @CacheInRedis(ttl = 30),     // 频繁变化的数据，TTL短
            cacheInJvm = @CacheInJvm(ttl = 10)
    )
    public Data getRealtime(String id) {
}
}
```

---

## 更多资源

- **完整示例代码**：[Demo Project](https://github.com/Dao1230source/demo.git)
- **源代码包**：`org.source.spring.cache`

- **核心类（实现要点）**：
    - `@ConfigureCache` - 主注解，继承自 `@Cacheable`，支持批量缓存和部分缓存策略
    - `@EnableExtendedCache` - 启用扩展缓存（会扫描 basePackages，并在 Redis 自动配置之后导入相关配置）
    - `ConfigureCacheInterceptor` - 拦截器实现，处理 `PartialCacheResult` 并在 PARTIAL_TRUST 模式下对缺失 key 进行二次查询合并
    - `ConfigureRedisCache` - 自定义 RedisCache，支持 mGet/mPut、批量缓存处理、JVM+Redis 二级缓存协调、NullValue 处理、分片存储
    - `ConfigureRedisCacheManager` - 负责创建 `ConfigureRedisCache` 的 CacheManager（事务感知）
    - `ConfigureRedisCacheWriter` - 低级读写实现，提供 mGet/mPut/mRemove 与 Pub/Sub 发布能力、HSET 分片读写
    - `ConfigureCacheConfig` - 将注解配置转为运行时 `ConfigureCacheProperties` 并注册缓存相关 Bean
    - `ConfigureInterceptorConfig` - 注册自定义拦截器与缓存解析器
    - `ConfigureCacheProperties` - 每个 cache 的运行时属性（TTL、是否启用 JVM 缓存、value 类型、分片策略等）
    - `ConfigureTtlProperties` - 全局 TTL 配置（用于默认值来源）
    - `ConfigureCacheUtil` - 若干辅助方法（SpEL 解析、类型判断等）
    - `PartialCacheStrategyEnum` - 部分缓存策略枚举（TRUST/DISTRUST/PARTIAL_TRUST）
    - `PartialCacheResult` - PARTIAL_TRUST 时返回的包装，包含已缓存的 key 列表
    - `ShardStrategyEnum` - 分片策略枚举（NONE/FIXED_SHARD/FIXED_SIZE）

实现要点：

- 对于批量缓存（方法返回 Collection/Map）需要设置 `cacheKeySpEl`，用于从单条数据中提取缓存 key。
- 当 `@ConfigureCache.key()` 为空时，框架会使用方法的第一个参数作为 key 的来源；PARTIAL_TRUST 的再查询逻辑依赖于能修改该入参（必须为可变
  Collection）。如果显式设置了 `key()`，框架无法可靠修改入参引用，PARTIAL_TRUST 将无法正常工作。
- 对于 null 值，框架使用 `NullValue` 占位（受 `allowNullValues` 配置影响），并在 JVM/Redis 两侧分别做相应处理。

更多实现细节请参考源码中的 `ConfigureRedisCache`, `ConfigureCacheInterceptor`, `ConfigureRedisCacheWriter` 和
`ConfigureCacheConfig` 等类。

---

## 与其他缓存框架对比

| 特性 | 本框架 | Spring Cache 原生 | Caffeine | Redis Cache |
|------|--------|------------------|----------|-------------|
| 批量缓存（Multi-Get） | ✅ 支持 | ❌ 不支持 | ❌ 不支持 | ❌ 不支持 |
| 二级缓存（L1+L2） | ✅ 内置支持 | ❌ 需手动实现 | 仅L1 | 仅L2 |
| 部分缓存策略 | ✅ 3种策略 | ❌ 不支持 | ❌ 不支持 | ❌ 不支持 |
| 分布式缓存同步 | ✅ 自动Pub/Sub | ❌ 需手动实现 | ❌ 不支持 | ❌ 不支持 |
| Redis 分片缓存 | ✅ 2种策略 | ❌ 不支持 | ❌ 不支持 | ❌ 不支持 |
| 泛型类型支持 | ✅ 完整支持 | ⚠️ 有限支持 | ⚠️ 有限支持 | ⚠️ 有限支持 |
| Spring注解兼容 | ✅ 完全兼容 | ✅ 原生支持 | ✅ 支持 | ✅ 支持 |

---

## 常见问题（FAQ）

### Q1: 批量缓存方法入参必须是可变集合吗？

**A**: 取决于 `partialCacheStrategy` 配置：

| 策略 | 入参要求 | 说明 |
|------|----------|------|
| `PARTIAL_TRUST`（默认） | **必须是可变 Collection** | 如 `ArrayList`，不能用 `List.of()` 或 `Arrays.asList()` |
| `DISTRUST` | 无限制 | 任何类型都可以 |
| `TRUST` | 无限制 | 任何类型都可以 |

```java
public class Example {
    public void partialCacheStrategy() {
      // ✅ 正确：PARTIAL_TRUST 使用可变集合
      List<String> ids = new ArrayList<>();
      ids.add("1");
      ids.add("2");
      service.getStudents(ids);

      // ❌ 错误：PARTIAL_TRUST 使用不可变集合会抛异常
      service.getStudents(List.of("1", "2"));  // UnsupportedOperationException

      // ✅ 正确：DISTRUST 可以使用任意集合
      service.getStudents(List.of("1", "2"));
}

}
```

### Q2: 如何处理缓存 null 值？

**A**: 框架自动使用 `NullValue` 占位符处理 null 值，无需额外配置。

```java
public class Example {
    @ConfigureCache(cacheNames = "users", key = "#id")
    public User getUserById(String id) {
        return userRepository.findById(id).orElse(null);  // null 也会被缓存
}
}
```

### Q3: 如何在 @Transactional 环境下使用缓存？

**A**: 框架已内置事务感知，缓存操作会参与当前事务：

```java
public class Example {
    @Transactional
    @ConfigureCache(cacheNames = "orders", key = "#id")
    public Order getOrder(String id) {
        // 缓存操作在事务上下文中执行
        return orderRepository.findById(id);
}
}
```

### Q4: 二级缓存的查询顺序是什么？

**A**: JVM（L1）→ Redis（L2）→ 数据源

1. 先查询 JVM 本地缓存（最快，无网络开销）
2. JVM 未命中 → 查询 Redis
3. Redis 未命中 → 执行业务方法
4. 结果同时存入 JVM 和 Redis

### Q5: 如何自定义缓存 key 序列化？

**A**: 通过 `CacheKeySpEl` 自定义 key 提取逻辑：

```java
public class Example {
    // 使用对象属性作为 key
    @ConfigureCache(cacheNames = "users", cacheKeySpEl = "#R.email")
    public List<User> getUsersByEmails(Collection<String> emails) {}

    // 使用嵌套属性作为 key
    @ConfigureCache(cacheNames = "orders", cacheKeySpEl = "#R.orderNo")
    public List<Order> getOrders(Collection<String> orderIds) {}
}
```

### Q6: 如何禁用 Redis 缓存仅使用 JVM 缓存？

**A**: 配置 `cacheInRedis.enable = false`：

```java
public class Example {
    @ConfigureCache(
        cacheNames = "tempData",
        cacheInRedis = @CacheInRedis(enable = false),
        cacheInJvm = @CacheInJvm(enable = true, ttl = 60)
    )
    public TempData getTempData(String id) {}
}
```

### Q7: 缓存失效如何同步到其他实例？

**A**: 使用 `@CacheEvict` 会自动触发 Pub/Sub 通知：

```java
public class Example {
    @CacheEvict(cacheNames = "users", key = "#id")
    public void updateUser(String id, User user) {
        userRepository.save(user);
        // 自动通知所有实例删除 JVM 缓存
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

`spring-cache` `redis-cache` `jvm-cache` `two-level-cache` `batch-cache` `multi-get` `distributed-cache` `sharded-cache` `redis-hset` `spring-boot` `spring-boot-starter` `cache-extension` `partial-cache` `缓存增强` `批量缓存` `二级缓存` `分布式缓存` `分片缓存` `Spring缓存扩展`