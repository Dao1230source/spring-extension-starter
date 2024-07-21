## spring-cache-configure-starter主要是对 `@Cacheable` 增强

### 注解属性介绍

- `@ConfigureCache.cacheNames()` 同 `@Cacheable.cacheNames()`
- `@ConfigureCache.key()` 同 `@Cacheable.key()`
- `@ConfigureCache.key()` 同 `@Cacheable.key()`
- `@ConfigureCache.cacheResolver()` 同 `@Cacheable.cacheResolver()`
- `@ConfigureCache.condition()` 同 `@Cacheable.condition()`
- `@ConfigureCache.cacheKeySpEl()`
  ```text
  SpEL表达式
  当方法返回值是List/ Set时必填
  value生成redis key的spEl表达式
  使用 #P 来表示 spEL 中的占位符
  ```
- `@ConfigureCache.returnType()`
    ```text
      ConfigureCacheConfig#assignValueClassesAndReturnType(Method, ConfigureCacheProperties)
      会自动判断value是否是容器类型进行判断
      只有当缓存值确实是 Collection/ Map时才需要指定ReturnTypeEnum. RAW
    ```
- `@ConfigureCache.partialCacheStrategy()`
  批量执行部分key不能从缓存中获取值时的处理策略，详情见`org.source.spring.cache.strategy.PartialCacheStrategyEnum`
- `@ConfigureCache.cacheInRedis()`
    - `@CacheInRedis.enable()` 是否缓存在redis
    - `@CacheInRedis.ttl()` redis过期时间，秒(s)，优先注解配置，默认从配置文件读取
      ```properties
      org.source.cache.redis-ttl=300
      ```
    - `@CacheInRedis.valueClasses()`
      ```text
      redis上缓存的单条数据的java类型
      当key单条时，value类型=方法返回值类型
      当key批量时，value类型=方法返回值容器中对象 （Map<K,V>的V,List<E>,Set<E>的E）的类型
      ```
- `@ConfigureCache.cacheInJvm()`
    - `@CacheInRedis.enable()` 是否缓存在redis
    - `@CacheInRedis.ttl()` redis过期时间，秒(s)，优先注解配置，默认从配置文件读取
      ```properties
      org.source.cache.jvm-ttl=300
      ```
    - `@CacheInRedis.jvmMaxSize()` 缓存的最大数量，默认`-1`不限制数量
    - `@CacheInRedis.keyClass()` 缓存的最大数量
      ```text
      单条数据时，key=ConfigureCache. key()指定的值
      多条数据时，
        返回值是 Map<K,V>，key=K
        返回值是集合时，key=经过ConfigureCache.cacheKeySpEl()计算之后的 List<E>/ Set<E>的E
      ```  

### 如何使用

- 原spring cache注解`@Cacheable、@Caching、@CacheEvict、@CachePut`等照旧使用
- 支持批量功能即方法入参是Collection类型
    - 返回值是List/Set
      ```java
      @ConfigureCache(cacheNames = "strings2ViewList", key = "#names", cacheKeySpEl = "#P.name")
      public List<StudentView> strings2ViewList(Collection<String> names) {
      }
      ```
    - 返回值是Map
      ```java
      @ConfigureCache(cacheNames = "str2ViewMap", key = "#names")
      public Map<String, StudentView> str2ViewMap(Collection<String> names) {
      }
      ```
    - 当部分key不能从缓存中获取值时
      ```java
      @ConfigureCache(cacheNames = "partialCacheStrategyPartialTrust", key = "#names", cacheKeySpEl = "#P.name",
            partialCacheStrategy = PartialCacheStrategyEnum.PARTIAL_TRUST)
      public List<StudentView> partialCacheStrategyPartialTrust(Collection<String> names) {
      }
      ```    
- 支持缓存value为参数化类型
    ```java
    @ConfigureCache(cacheNames = "str2ViewListCacheInJvm", key = "#className", returnType = ReturnTypeEnum.RAW,
            cacheInRedis = @CacheInRedis(enable = false),
            cacheInJvm = @CacheInJvm(enable = true))
    public List<StudentView> str2ViewListCacheInJvm(String className) {
    }
    ```

- 支持二级缓存
  ```java
  
  @ConfigureCache(cacheNames = "str2strCacheInJvm", cacheInJvm = @CacheInJvm(enable = true))
  public String str2strCacheInJvm(String str) {
  }
  ```

更多使用请见 [demo](http://)

### 辅助信息

设置日志等级为debug
```properties
logging.level.org.source=debug
```
搜索以下关键字，可确认是否执行关键操作

- `ConfigureCache get from jvm,key` 从jvm中获取数据
- `ConfigureCache get from redis, key` 从redis中获取数据
- `ConfigureCache put jvm` 保存数据到jvm
- `ConfigureCache put redis` 保存数据到redis
- `ConfigureCache publish evict message, receive clients` 通知集群的其他实例删除本地缓存
- `ConfigureCache evict jvm via pubsub` 接到通知删除本地缓存
- `ConfigureCache invoke method again` 以未获取到缓存的keys为参数重新执行方法
