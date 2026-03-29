# Spring Extension Doc - API 文档生成组件

> **关键词（Keywords）**: JavaDoc, API Documentation, Code Documentation, REST API Docs, Swagger Alternative, Build-time Documentation, API文档生成, 代码文档, 构建时文档

[![Maven Central](https://img.shields.io/maven-central/v/io.github.dao1230source/spring-extension-starter-doc.svg)](https://central.sonatype.com/artifact/io.github.dao1230source/spring-extension-starter-doc)
[![Java](https://img.shields.io/badge/Java-21+-green.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)

## 概述

基于 JavaDoc 的 API 文档生成系统，从 Java 源码提取结构化文档数据：

- **源码解析**：通过自定义 Doclet 解析 Java 源码
- **文档树构建**：生成类、方法、字段、参数的层级文档
- **继承关系处理**：自动合并父类文档信息
- **访问级别控制**：支持按访问级别过滤文档

**与 Swagger 的区别**：本模块在**构建时**生成文档，无需运行时扫描，性能更高且不侵入代码。

---

## 核心功能

### 1. 文档数据容器（DocDataContainer）

统一管理所有提取的文档数据：

```java
public class Example {
    public void example() {
        DocDataContainer container = new DocDataContainer();
        container.addClassData(classData);
        container.addMethodData(methodData);
    }
}
```

### 2. 文档处理器（AbstractDocProcessor）

核心处理逻辑，构建文档树并处理继承关系：

```java
public class MyDocProcessor extends AbstractDocProcessor {
    @Override
    protected void processClass(DocClassData classData) {
        // 处理类文档
    }
}
```

### 3. 自定义 Doclet

提供多种 Doclet 支持不同文档类型：

| Doclet | 说明 |
|--------|------|
| `RequestDoclet` | 处理 REST API 请求，提取方法签名、参数、返回类型 |
| `VariableDoclet` | 处理类字段和变量，包含继承关系 |

### 4. 配置选项（MyOptions）

灵活配置文档生成参数：

```java
public class Example {
    public void example() {
        MyOptions options = new MyOptions();
        options.setProjectDir("/path/to/project");
        options.setClassName("com.example.*");
        options.setAccessLevel("public");
    }
}
```

---

## 快速开始

### 添加依赖

**Maven:**

```xml
<dependency>
    <groupId>io.github.dao1230source</groupId>
    <artifactId>spring-extension-starter-doc</artifactId>
    <version>0.0.12</version>
</dependency>
```

### 使用示例

#### 通过 Maven 插件生成文档

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-javadoc-plugin</artifactId>
    <configuration>
        <doclet>org.source.spring.doc.doclet.RequestDoclet</doclet>
        <docletArtifact>
            <groupId>io.github.dao1230source</groupId>
            <artifactId>spring-extension-starter-doc</artifactId>
            <version>0.0.12</version>
        </docletArtifact>
    </configuration>
</plugin>
```

#### 编程方式使用

```java
public class Example {
    public void example() {
        // 创建配置
        MyOptions options = new MyOptions();
        options.setProjectDir(System.getProperty("user.dir"));
        options.setOutputDir("target/docs");

        // 执行文档处理
        AbstractDocProcessor processor = new MyDocProcessor();
        processor.process(options);
    }
}
```

---

## 文档数据结构

### DocData 基类

所有文档数据的基类：

```java
public class Example {
    public abstract class DocData {
        private String name;        // 名称
        private String comment;     // JavaDoc 注释
        private DocData parent;     // 父节点
        private List<DocData> children; // 子节点列表
    }
}
```

### 具体数据类型

| 类型 | 说明 |
|------|------|
| `DocClassData` | 类/接口文档数据 |
| `DocMethodData` | 方法文档数据 |
| `DocFieldData` | 字段文档数据 |
| `DocParameterData` | 参数文档数据 |
| `DocRequestData` | REST API 请求数据 |

---

## 与其他方案对比

| 特性 | 本模块 | Swagger/OpenAPI | Spring REST Docs |
|------|--------|-----------------|------------------|
| 生成时机 | 构建时 | 运行时 | 测试时 |
| 性能影响 | 无 | 有 | 有 |
| 代码侵入 | 无 | 需注解 | 需测试代码 |
| 继承支持 | ✅ | ❌ | ❌ |
| 字段文档 | ✅ | ❌ | ❌ |

---

## 适用场景

- 需要从源码自动生成 API 文档
- 不希望运行时扫描影响性能
- 需要完整的继承关系文档
- 构建时文档生成流水线
- 替代 Swagger 的轻量级方案

---

## 相关资源

- **GitHub 仓库**: [spring-extension-starter](https://github.com/Dao1230source/spring-extension-starter)
- **示例代码**: [Demo Project](https://github.com/Dao1230source/demo)
- **问题反馈**: [GitHub Issues](https://github.com/Dao1230source/spring-extension-starter/issues)

---

## 标签（Tags）

`javadoc` `api-documentation` `code-documentation` `rest-api-docs` `swagger-alternative` `build-time-docs` `spring-boot` `文档生成` `API文档` `代码文档`