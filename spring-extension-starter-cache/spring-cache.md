# org.source.spring.cache

## 概述

基于Spring Cache框架的增强扩展，提供以下核心功能：

- **批量缓存获取**：支持Collection入参的批量方法，一次性获取多个缓存项
- **二级缓存架构**：支持JVM本地缓存+Redis远程缓存的多级缓存策略
- **智能缓存同步**：基于Redis Pub/Sub的分布式缓存失效通知机制
- **灵活的部分缓存处理**：当部分数据缺失时的多种处理策略
- **参数化类型支持**：支持Generic类型的缓存存储和反序列化
- [架构UML图](Spring_Extension_Cache_Architecture.png)

**支持的Spring Cache原生注解**：`@Cacheable`、`@Caching`、`@CacheEvict`、`@CachePut`等都可以正常使用

---

## 快速开始

### 1. 启用扩展缓存

在Application类上添加注解：

```java
@SpringBootApplication
@EnableExtendedCache
public class YourApplication {
    public static void main(String[] args) {
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
@ConfigureCache(cacheNames = "users", key = "#id")
public User getUserById(String id) {
    return userService.fetchFromDb(id);
}
```

---

## 核心注解详解

### @ConfigureCache - 主注解

标注在方法上，声明该方法需要缓存。继承自`@Cacheable`，支持所有原生属性。

#### 基础属性（继承自@Cacheable）

| 属性 | 类型 | 说明 |
|------|------|------|
| `cacheNames()` | String[] | 缓存名称，必填 |
| `key()` | String | SpEL表达式，定义缓存key |
| `cacheResolver()` | String | CacheResolver bean名称 |
| `condition()` | String | SpEL条件表达式，满足时才缓存 |

#### 扩展属性

| 属性 | 类型 | 说明 |
|------|------|------|
| `cacheKeySpEl()` | String | **批量缓存专用** - 从返回值中提取单条数据的key的SpEL表达式 |
| `returnType()` | ReturnTypeEnum | 返回值类型，支持LIST、MAP、SET、RAW、AUTO |
| `partialCacheStrategy()` | PartialCacheStrategyEnum | 部分数据缺失时的处理策略 |
| `cacheInRedis()` | @CacheInRedis | Redis缓存配置 |
| `cacheInJvm()` | @CacheInJvm | JVM缓存配置 |

#### cacheKeySpEl() 详解

当方法返回Collection或Map时，需要指定`cacheKeySpEl()`来定义如何从单条数据中提取缓存key。

使用`#R`占位符表示返回值中的单条数据：

```java
// 返回List<Student>，key为student.id
@ConfigureCache(
    cacheNames = "students",
    key = "#ids",
    cacheKeySpEl = "#R.id"  // 从Student对象中提取id作为缓存key
)
public List<Student> getStudentsByIds(List<String> ids) { }

// 返回Set<User>，key为user.email
@ConfigureCache(
    cacheNames = "users",
    key = "#emails",
    cacheKeySpEl = "#R.email"
)
public Set<User> getUsersByEmails(Collection<String> emails) { }
```

#### returnType() 详解

自动类型判断机制：

| 值 | 适用场景 |
|----|---------|
| `AUTO`（默认） | 框架自动判断，推荐使用 |
| `LIST` | 返回值是List<E> |
| `SET` | 返回值是Set<E> |
| `MAP` | 返回值是Map<K,V> |
| `RAW` | 返回值本身是Collection/Map的包装类，如`List<StudentView>` |

---

### @CacheInRedis - Redis缓存配置

```java
@interface CacheInRedis {
    // 是否启用Redis缓存，默认true
    boolean enable() default true;

    // 过期时间（秒），默认从配置文件读取
    long ttl() default CacheConstant.FROM_CONFIG;

    // 缓存值的Java类型（用于反序列化）
    Class<?>[] valueClasses() default {};
}
```

#### valueClasses() 说明

指定Redis中存储的值类型，用于JSON反序列化：

- **单条缓存**：valueClasses不需要指定，自动使用方法返回值类型
- **批量缓存**：指定容器内元素的类型
  - `Map<K,V>`返回值：valueClasses = {V.class}
  - `List<E>`返回值：valueClasses = {E.class}
  - `Set<E>`返回值：valueClasses = {E.class}

```java
// 返回Map<String, Student>
@ConfigureCache(
    cacheNames = "students",
    key = "#ids",
    cacheInRedis = @CacheInRedis(valueClasses = {Student.class})
)
public Map<String, Student> getStudentMap(List<String> ids) { }
```

---

### @CacheInJvm - JVM本地缓存配置

```java
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
```

#### keyClass() 说明

指定JVM缓存中key的类型：

- **单条缓存**：key = @ConfigureCache.key()的值
- **批量缓存**：
  - 返回`Map<K,V>`：key = K
  - 返回`List<E>/Set<E>`：key = 通过cacheKeySpEl提取的E值

```java
// JVM中的key为String类型（学生ID）
@ConfigureCache(
    cacheNames = "students",
    key = "#ids",
    cacheKeySpEl = "#R.id",
    cacheInJvm = @CacheInJvm(enable = true, jvmMaxSize = 10000)
)
public List<Student> getStudents(List<String> ids) { }
```

---

## 使用场景详解

### 场景1：单条缓存（简单使用）

```java
@ConfigureCache(cacheNames = "user", key = "#id")
public User getUserById(String id) {
    return userService.findById(id);
}
```

- 自动缓存到Redis（如已配置）
- 每次调用时先查询缓存，缓存命中返回

### 场景2：批量缓存 - 返回List

当需要批量查询多个资源时：

```java
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
```

### 场景3：批量缓存 - 返回Map

当返回值是Map时，key自动从Map中提取：

```java
@ConfigureCache(
    cacheNames = "studentMap",
    key = "#ids",
    cacheInRedis = @CacheInRedis(valueClasses = {Student.class})
)
public Map<String, Student> getStudentMapByIds(Collection<String> ids) {
    // Map的key会自动作为缓存key
    return studentService.findMapByIds(ids);
}
```

### 场景4：二级缓存架构

同时使用JVM和Redis缓存，提高查询性能：

```java
@ConfigureCache(
    cacheNames = "hotUsers",
    key = "#id",
    cacheInRedis = @CacheInRedis(ttl = 3600),  // Redis缓存1小时
    cacheInJvm = @CacheInJvm(enable = true, ttl = 300, jvmMaxSize = 5000)  // JVM缓存5分钟
)
public User getHotUser(String id) {
    return userService.findById(id);
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
@ConfigureCache(
    cacheNames = "students",
    key = "#ids",
    cacheKeySpEl = "#R.id",
    partialCacheStrategy = PartialCacheStrategyEnum.TRUST
)
public List<Student> getStudents(List<String> ids) { }
```

- 缓存命中的数据直接返回
- 缺失的key**不会再次查询**，只返回缓存中存在的数据

### 场景6：部分缓存策略 - DISTRUST（默认）

不信任缓存，任何缺失都重新调用方法：

```java
@ConfigureCache(
    cacheNames = "students",
    key = "#ids",
    cacheKeySpEl = "#R.id",
    partialCacheStrategy = PartialCacheStrategyEnum.DISTRUST
)
public List<Student> getStudents(Collection<String> ids) { }
```

- 如果部分key缺失，会用缺失的key重新调用此方法
- 适合对一致性要求高的场景

### 场景7：部分缓存策略 - PARTIAL_TRUST

智能处理：缓存命中+对缺失数据重新查询：

```java
@ConfigureCache(
    cacheNames = "students",
    key = "#ids",  // 必须指向第一个参数或为空
    cacheKeySpEl = "#R.id",
    partialCacheStrategy = PartialCacheStrategyEnum.PARTIAL_TRUST
)
public List<Student> getStudents(Collection<String> ids) { }
```

**使用限制**：
- 方法必须有且仅有一个参数
- 入参必须是可变Collection（不能是`List.of()`创建的不可变列表）
- `key()`无值或指向第一个参数

**优势**：避免不必要的完整重查，性能最优

### 场景8：JVM缓存参数化类型

缓存复杂的泛型对象：

```java
@ConfigureCache(
    cacheNames = "classCache",
    key = "#className",
    returnType = ReturnTypeEnum.RAW,  // 显式声明返回类型是容器
    cacheInRedis = @CacheInRedis(enable = false),
    cacheInJvm = @CacheInJvm(enable = true)
)
public List<StudentView> getStudentsByClass(String className) { }
```

---

## 分布式场景 - 缓存失效同步

在分布式环境中，当一个实例更新缓存时，需要通知其他实例删除本地缓存。

框架通过Redis Pub/Sub自动实现：

```java
// 更新操作，使用@CacheEvict删除缓存
@CacheEvict(cacheNames = "students", key = "#id")
public void updateStudent(String id, StudentForm form) {
    studentService.update(id, form);
    // 框架自动发布Pub/Sub消息到所有实例
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

| 日志内容 | 含义 |
|---------|------|
| `ConfigureCache get from jvm,key` | 从JVM缓存命中 |
| `ConfigureCache get from redis, key` | 从Redis缓存命中 |
| `ConfigureCache put jvm` | 数据保存到JVM |
| `ConfigureCache put redis` | 数据保存到Redis |
| `ConfigureCache publish evict message, receive clients` | 发布缓存失效通知 |
| `ConfigureCache evict jvm via pubsub` | 接收到缓存失效通知 |
| `ConfigureCache invoke method again` | 部分缓存缺失，重新执行方法 |

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
// 热点数据 → 二级缓存
@ConfigureCache(
    cacheNames = "hotItems",
    key = "#id",
    cacheInJvm = @CacheInJvm(enable = true),
    cacheInRedis = @CacheInRedis(ttl = 3600)
)
public Item getHotItem(String id) { }

// 冷数据 → 仅Redis
@ConfigureCache(
    cacheNames = "coldItems",
    key = "#id",
    cacheInRedis = @CacheInRedis(ttl = 86400)
)
public Item getColdItem(String id) { }

// 临时数据 → 仅JVM
@ConfigureCache(
    cacheNames = "tempData",
    key = "#id",
    cacheInJvm = @CacheInJvm(enable = true, ttl = 60),
    cacheInRedis = @CacheInRedis(enable = false)
)
public TempData getTempData(String id) { }
```

### 2. 批量查询合理配置

```java
// ❌ 不好：过大的批量，容易导致单次查询缓存太多
@ConfigureCache(cacheNames = "users", key = "#ids", cacheKeySpEl = "#R.id")
public List<User> getUsersByIds(Collection<String> ids) { }

// ✅ 好：在Service层限制批量大小
public List<User> getUsersByIds(Collection<String> ids) {
    if (ids.size() > 100) {
        // 分批处理
        return ids.stream()
            .collect(Collectors.groupingBy(id -> ..., Collectors.toList()))
            .values().stream()
            .flatMap(batch -> getUsersBatch(batch).stream())
            .collect(Collectors.toList());
    }
    return getUsersBatch(ids);
}

@ConfigureCache(cacheNames = "users", key = "#ids", cacheKeySpEl = "#R.id")
private List<User> getUsersBatch(Collection<String> ids) { }
```

### 3. 部分缓存策略的选择

```java
// DISTRUST（默认）：数据一致性最高，但可能重复查询
@ConfigureCache(
    cacheNames = "orders",
    partialCacheStrategy = PartialCacheStrategyEnum.DISTRUST
)
public List<Order> getOrders(Collection<String> orderIds) { }

// PARTIAL_TRUST：性能最优，但有分布式事务风险
@ConfigureCache(
    cacheNames = "products",
    partialCacheStrategy = PartialCacheStrategyEnum.PARTIAL_TRUST
)
public List<Product> getProducts(Collection<String> productIds) { }

// TRUST：适合允许短期不一致的场景（推荐使用少）
@ConfigureCache(
    cacheNames = "stats",
    partialCacheStrategy = PartialCacheStrategyEnum.TRUST
)
public List<Stat> getStats(Collection<String> statIds) { }
```

### 4. 明确指定valueClasses

```java
// ❌ 不好：未指定，可能反序列化失败
@ConfigureCache(cacheNames = "students", key = "#ids", cacheKeySpEl = "#R.id")
public List<StudentView> getStudents(List<String> ids) { }

// ✅ 好：明确指定元素类型
@ConfigureCache(
    cacheNames = "students",
    key = "#ids",
    cacheKeySpEl = "#R.id",
    cacheInRedis = @CacheInRedis(valueClasses = {StudentView.class})
)
public List<StudentView> getStudents(List<String> ids) { }
```

### 5. 合理设置TTL

```java
// 根据数据更新频率设置不同的TTL
org.source.spring.cache.redis-ttl=300      # 默认5分钟
org.source.spring.cache.jvm-ttl=60         # JVM缓存更短

// 或在注解中覆盖
@ConfigureCache(
    cacheNames = "realtime",
    cacheInRedis = @CacheInRedis(ttl = 30),     # 频繁变化的数据，TTL短
    cacheInJvm = @CacheInJvm(ttl = 10)
)
public Data getRealtime(String id) { }
```

---

## 更多资源

- **完整示例代码**：[Demo Project](https://github.com/Dao1230source/demo.git)
- **源代码包**：`org.source.spring.cache`
- **核心类**：
  - `@ConfigureCache` - 主注解
  - `ConfigureCacheInterceptor` - 拦截器实现
  - `ConfigureRedisCacheManager` - Redis缓存管理
  - `PartialCacheStrategyEnum` - 部分缓存策略枚举
  - `PartialCacheResult` - 部分缓存结果包装类