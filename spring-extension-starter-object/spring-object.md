# Spring Extension Object - 对象树管理组件

> **关键词（Keywords）**: Object Tree, Hierarchical Data, DAG, Multi-Parent, Tree Structure, Relation Management, Batch Operations, 对象树, 层级数据, 多父节点, 关系管理, 批量操作

[![Maven Central](https://img.shields.io/maven-central/v/io.github.dao1230source/spring-extension-starter-object.svg)](https://central.sonatype.com/artifact/io.github.dao1230source/spring-extension-starter-object)
[![Java](https://img.shields.io/badge/Java-21+-green.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)

## 概述

层级对象数据管理框架，提供：

- **树形结构管理**：支持父子关系、多父节点（DAG）
- **批量操作**：批量保存、合并、删除、查询
- **关系管理**：支持多种关系类型和作用域
- **事务支持**：集成 Spring 事务
- **状态追踪**：追踪对象状态（新建、缓存、持久化）

**适用于文档结构、分类体系、组织架构等层级数据场景**。

---

## 快速开始

### 1. 添加依赖

**Maven:**

```xml
<dependency>
    <groupId>io.github.dao1230source</groupId>
    <artifactId>spring-extension-starter-object</artifactId>
    <version>0.0.12</version>
</dependency>
```

### 2. 定义对象实体

```java
@Entity
public class DocumentEntity implements ObjectEntityDefiner {
    @Id
    private Long id;
    private String name;
    private Long parentId;
    private String objectType;
    // ...
}
```

### 3. 实现处理器

```java
@Service
public class DocumentProcessor extends AbstractObjectProcessor<DocumentElement> {
    
    @Override
    protected void doSave(List<DocumentElement> elements) {
        // 保存到数据库
    }
    
    @Override
    protected List<DocumentElement> doFind(Collection<Long> ids) {
        // 从数据库查询
    }
}
```

### 4. 使用对象树

```java
@Service
public class DocumentService {
    
    @Autowired
    private DocumentProcessor processor;
    
    public void saveDocumentTree(List<DocumentElement> elements) {
        // 批量保存对象树
        processor.save(elements);
    }
    
    public DocumentElement findById(Long id) {
        return processor.find(id);
    }
}
```

---

## 核心类详解

### ObjectElement - 对象元素

表示树中的节点：

```java
public class ObjectElement {
    private String name;           // 名称
    private Object value;          // 值
    private String type;           // 类型
    private List<Relation> relations; // 关系列表
    private StatusEnum status;     // 状态
    private Long createTime;
    private Long updateTime;
}
```

### ObjectNode - 树节点

继承 `EnhanceNode`，支持多父节点：

```java
public class ObjectNode extends EnhanceNode<Long, ObjectElement, ObjectNode> {
    private StatusEnum status;      // 状态
    private RelationTypeEnum relationType; // 关系类型
}
```

### AbstractObjectProcessor - 抽象处理器

提供核心操作能力：

```java
public class Example {
    public abstract class AbstractObjectProcessor<E extends ObjectElement> {
    
        // 批量保存
        public void save(Collection<E> elements);
    
        // 合并（更新已存在，新增不存在）
        public void merge(Collection<E> elements);
    
        // 批量删除
        public void delete(Collection<Long> ids);
    
        // 查询单个
        public E find(Long id);
    
        // 批量查询
        public List<E> find(Collection<Long> ids);
    }
}
```

---

## 状态追踪

### StatusEnum - 对象状态

| 状态 | 说明 |
|------|------|
| `DATABASE` | 已持久化到数据库 |
| `CREATED` | 新创建，未持久化 |
| `CACHED` | 从缓存加载 |
| `DELETED` | 已删除 |

### 状态变化追踪

```java
public class Example {
    public void example() {
        ObjectElement element = processor.find(id);
        // element.status == DATABASE

        element.setValue(newValue);
        processor.merge(List.of(element));
        // 自动更新状态
    }
}
```

---

## 关系管理

### RelationTypeDefiner - 关系类型

```java
public enum DocumentRelationType implements RelationTypeDefiner {
    PARENT_CHILD("父子关系"),
    REFERENCE("引用关系"),
    DEPENDENCY("依赖关系")
}
```

### RelationScopeEnum - 关系作用域

| 作用域 | 说明 |
|--------|------|
| `GLOBAL` | 全局关系 |
| `LOCAL` | 局部关系 |

---

## 使用场景

### 场景1：文档结构管理

```java
@Service
public class DocumentService {
    
    @Autowired
    private DocumentProcessor processor;
    
    public void saveDocumentWithChildren(Document doc, List<Document> children) {
        List<ObjectElement> elements = new ArrayList<>();
        
        // 父文档
        ObjectElement parent = new ObjectElement();
        parent.setName(doc.getName());
        elements.add(parent);
        
        // 子文档
        for (Document child : children) {
            ObjectElement element = new ObjectElement();
            element.setName(child.getName());
            element.setParentId(doc.getId());
            elements.add(element);
        }
        
        processor.save(elements);
    }
}
```

### 场景2：分类体系

```java
@Service
public class CategoryService {
    
    @Autowired
    private CategoryProcessor processor;
    
    public CategoryTree getCategoryTree() {
        List<ObjectElement> all = processor.findAll();
        // 构建树结构
        CategoryTree tree = buildTree(all);
        return tree;
    }
}
```

### 场景3：组织架构（多父节点）

```java
public class Example {
    public void example() {
        // 员工属于多个部门
        ObjectElement employee = new ObjectElement();
        employee.setId(1L);
        employee.setName("张三");

        // 添加多个父节点
        employee.addRelation(2L, RelationType.DEPARTMENT);  // 技术部
        employee.addRelation(3L, RelationType.DEPARTMENT);  // 产品部

        processor.save(List.of(employee));
    }
}
```

---

## 核心接口

| 接口 | 说明 |
|------|------|
| `ObjectDbHandlerDefiner` | 对象数据库操作接口 |
| `ObjectBodyDbHandlerDefiner` | 对象内容数据库操作 |
| `RelationDbHandlerDefiner` | 关系数据库操作 |
| `ObjectTypeHandlerDefiner` | 对象类型处理 |
| `ObjectEntityDefiner` | 对象实体定义 |
| `RelationEntityDefiner` | 关系实体定义 |

---

## 配置属性

本模块无需额外配置，依赖注入即可使用。

---

## 相关资源

- **GitHub 仓库**: [spring-extension-starter](https://github.com/Dao1230source/spring-extension-starter)
- **示例代码**: [Demo Project](https://github.com/Dao1230source/demo)
- **问题反馈**: [GitHub Issues](https://github.com/Dao1230source/spring-extension-starter/issues)

---

## 标签（Tags）

`object-tree` `hierarchical-data` `dag` `multi-parent` `tree-structure` `relation-management` `batch-operations` `spring-boot-starter` `对象树` `层级数据` `关系管理`