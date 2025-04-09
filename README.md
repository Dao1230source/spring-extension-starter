## spring-extension-stater

jar包依赖

```xml

<dependency>
    <groupId>io.github.dao1230source</groupId>
    <artifactId>spring-extension-starter</artifactId>
    <version>latest</version>
</dependency>
```

spring-boot的一些扩展

### [spring.cache](./spring-cache.md)

基于spring cache的扩展，所有原注解和用法不变

- 支持批量获取缓存
- 支持返回值是`Collection/Map`类型
- 支持缓存值时泛型类型
- 支持本地+redis二级缓存
- 支持批量操作只获取部分缓存时进行二次重试

***如果方法加上事物，不会缓存数据，应优先解决***

### [国际化（I18N）](./国际化(I18N).md)

开箱即用的国际化组件
