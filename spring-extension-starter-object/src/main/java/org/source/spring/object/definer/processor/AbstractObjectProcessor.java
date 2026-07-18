package org.source.spring.object.definer.processor;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.source.spring.object.ObjectBodyData;
import org.source.spring.object.ObjectElement;
import org.source.spring.object.ObjectNode;
import org.source.spring.object.definer.entity.ObjectBodyDefiner;
import org.source.spring.object.definer.entity.ObjectDefiner;
import org.source.spring.object.definer.entity.RelationDefiner;
import org.source.spring.object.definer.enums.ObjectExceptionEnum;
import org.source.spring.object.definer.enums.ObjectTypeDefiner;
import org.source.spring.object.enums.StatusEnum;
import org.source.spring.object.mapper.ObjectElementMapper;
import org.source.utility.assign.Assign;
import org.source.utility.assign.InterruptStrategyEnum;
import org.source.utility.tree.EnhanceTree;
import org.source.utility.tree.define.IdExtend;
import org.source.utility.tree.define.Node;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Streams;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 建议通过 new 方式创建使用 processor，如果注册为spring bean，不同业务的数据会混杂在 objectTree 中增加处理难度
 *
 * @param <O> object 实例对象
 * @param <R> relation 实例对象
 * @param <B> object body 实例对象
 * @param <D> value 业务数据
 * @param <T> object 类型枚举
 */
@Getter
@Slf4j
public abstract class AbstractObjectProcessor<
        O extends ObjectDefiner, B extends ObjectBodyDefiner, R extends RelationDefiner, D extends ObjectBodyData, T extends ObjectTypeDefiner<D>>
        implements ObjectProcessor<O, B, R, D, T> {
    private final Function<ObjectNode<D>, @Nullable String> objectIdGetter = n -> Node.getProperty(n, e -> this.dataId(e.getData()));
    private final Function<ObjectNode<D>, @Nullable String> objectParentIdGetter = n -> Node.getProperty(n, e -> this.parentDataId(e.getData()));

    protected final EnhanceTree<String, ObjectElement<D>, ObjectNode<D>> objectTree = EnhanceTree.of(new ObjectNode<>());
    protected final IdExtend<String, ObjectElement<D>, ObjectNode<D>> treeIdExtendData = new IdExtend<>("data", objectIdGetter, objectParentIdGetter);

    @Override
    public void save(Collection<D> ds) {
        try {
            this.merge(ds);
            this.save();
        } finally {
            this.afterFinal();
        }
    }

    @Override
    public void delete(Collection<String> objectIds) {
        this.getObjectHandler().deleteObjects(objectIds);
    }

    @Override
    public void remove(Collection<String> objectIds) {
        Assign.build(objectIds)
                .cast(e -> ObjectTemp.<O, R>builder().objectId(e).build())
                .parallel().interruptStrategy(InterruptStrategyEnum.ANY)
                // 查询 object
                .addAcquire(this.getObjectHandler()::findObjects, O::getObjectId)
                .throwException()
                .addAction(ObjectTemp::getObjectId)
                .addAssemble(ObjectTemp::setObject)
                .backAcquire().backAssign().invoke()
                .cast(ObjectTemp::getObject)
                .addOperates(ObjectDefiner::getType, this.getObjectTypeHandler().typeObjectRemoveMap(), O::getObjectId);
    }

    @Override
    public ObjectNode<D> find(Collection<String> objectIds) {
        Collection<ObjectElement<D>> fullData = Assign.build(objectIds)
                // objectId 转为 ObjectTemp
                .cast(e -> ObjectTemp.<O, R>builder().objectId(e).build())
                .name("通过objectIds获取完整的数据")
                .parallel().interruptStrategy(InterruptStrategyEnum.ANY)
                // 查询 object
                .addAcquire(this.getObjectHandler()::findObjects, O::getObjectId)
                .name("获取object数据")
                .throwException()
                .addAction(ObjectTemp::getObjectId)
                .addAssemble(ObjectTemp::setObject)
                .backAcquire().backAssign()
                // 查询 relation 并按 belongId 分组
                .addAcquire(this.belongIdRelationsGroupMapping())
                .name("获取relation数据")
                .addAction(ObjectTemp::getObjectId)
                .addAssemble(ObjectTemp::setRelations)
                .backAcquire().backAssign().invoke()
                // ObjectTemp 转为 ObjectElement<ObjectBodyValueDefiner>
                .casts(es -> Streams.map(es, k -> {
                    if (Objects.isNull(k.getObject())) {
                        return List.<ObjectElement<D>>of();
                    }
                    ObjectElement<D> data = new ObjectElement<>();
                    O object = k.getObject();
                    data.setSpaceId(object.getSpaceId());
                    data.setType(object.getType());
                    D d = this.getObjectTypeHandler().getEmptyData(object.getType());
                    d.setObjectId(k.getObjectId());
                    data.setData(d);
                    if (Objects.isNull(k.getRelations())) {
                        return List.of(data);
                    }
                    return Streams.map(k.getRelations(), r -> {
                        @SuppressWarnings("unchecked")
                        ObjectElement<D> copy = (ObjectElement<D>) ObjectElementMapper.INSTANCE.copy((ObjectElement<ObjectBodyData>) data);
                        return copy;
                    }).collect(Collectors.toList());
                }).flatMap(Collection::stream).toList())
                .parallel().interruptStrategy(InterruptStrategyEnum.ANY)
                // 查询 objectBody
                .addBranches(ObjectElement::getType, this.getObjectTypeHandler().typeAssignerMap()).invoke()
                .toList();
        // 组成 tree 结构
        return EnhanceTree.of(new ObjectNode<D>()).add(fullData);
    }

    /**
     * 通过 AbstractObjectBodyData 获取 id，当 Tree 的计算id不是默认的objectId时dataId、dataParentId、objectBodyId都需要重写
     *
     * @param d data
     * @return id
     */
    public @Nullable String dataId(@Nullable D d) {
        return Objects.isNull(d) ? null : d.getObjectId();
    }

    public @Nullable String parentDataId(@Nullable D d) {
        return Objects.isNull(d) ? null : d.getParentObjectId();
    }

    public @Nullable String objectBodyId(@Nullable B b) {
        return Objects.isNull(b) ? null : b.getObjectId();
    }

    /**
     * 转为tree
     *
     * @param ds es
     */
    public void merge(Collection<D> ds) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("source values:{}", Jsons.str(ds));
            }
            Collection<D> maybeExistsInDb = this.validExists(ds);
            if (CollectionUtils.isNotEmpty(maybeExistsInDb)) {
                if (log.isDebugEnabled()) {
                    log.debug("maybeExistsInDb:{}", maybeExistsInDb);
                }
                // 从数据中查询数据并添加到tree中
                List<ObjectElement<D>> dataFromDbList = this.findFromDbAndConvertToObjectElement(maybeExistsInDb);
                if (log.isDebugEnabled()) {
                    log.debug("dataFromDbList:{}", dataFromDbList);
                }
                this.handleDbDataTree().add(dataFromDbList);
            }
            Collection<ObjectElement<D>> objectElements = Streams.map(ds, this::convertToObjectElement).toList();
            this.handleValueDataTree().add(objectElements);
            this.afterTransfer();
        } catch (Exception e) {
            this.getObjectTree().clear();
            log.error("AbstractObjectProcessor.merge error", e);
            ObjectExceptionEnum.OBJECT_MERGE_ERROR.throwException(e);
        }
    }

    /**
     * 需要校验是否在数据库是否存在的V
     *
     * @param ds vs
     * @return vs
     */
    public Collection<D> validExists(Collection<D> ds) {
        Map<String, ObjectNode<D>> idMap = this.getObjectTree().getIdMap();
        return Streams.retain(ds, d -> {
            String valueId = this.dataId(d);
            if (StringUtils.isEmpty(valueId)) {
                return true;
            }
            ObjectNode<D> node = idMap.get(valueId);
            return Objects.isNull(node) || !StatusEnum.DATABASE.equals(node.getStatus());
        }).toList();
    }

    /**
     * tree 保存数据库的数据
     *
     * @return tree
     */
    public EnhanceTree<String, ObjectElement<D>, ObjectNode<D>> handleDbDataTree() {
        EnhanceTree<String, ObjectElement<D>, ObjectNode<D>> tree = this.getObjectTree();
        // 默认 ObjectElement.getId() 作为 tree 关联的 id
        tree.setIdExtend(IdExtend.defaultIdExtend());
        // 另以指定的 dataId 为唯一键缓存，以便新的 data 对象加入时可以通过自定义的id找到缓存的Node
        tree.setExtendCacheIdExtends(List.of(treeIdExtendData));
        tree.setAfterCreateHandler(n -> n.setStatus(StatusEnum.DATABASE));
        tree.setMergeHandler(this::mergeNode);
        return tree;
    }

    public EnhanceTree<String, ObjectElement<D>, ObjectNode<D>> handleValueDataTree() {
        EnhanceTree<String, ObjectElement<D>, ObjectNode<D>> tree = this.getObjectTree();
        // 新的 data 对象加入时还未生成id，所以指定业务 dataId 作为 tree 关联的 id
        tree.setIdExtend(treeIdExtendData);
        tree.setExtendCacheIdExtends(List.of());
        // 如果没有指定 objectId，设置为 Uid
        tree.setAfterCreateHandler(n -> {
            n.setStatus(StatusEnum.CREATED);
            if (Objects.isNull(n.getId())) {
                Node.setProperty(n, e -> e.getData().setObjectId(this.getObjectUidHandler().getObjectId()));
            }
        });
        tree.setMergeHandler(this::mergeNode);
        return tree;
    }

    public void afterTransfer() {
    }

    /**
     * 持久化数据
     */
    public void save() {
        this.beforePersist();
        List<ObjectNode<D>> objectNodes = this.obtainObjectData();
        if (CollectionUtils.isEmpty(objectNodes)) {
            return;
        }
        ObjectNodesToEntitiesTemp<O, B, R> temp = this.nodeConvertToEntities(objectNodes);
        List<O> objectList = temp.getObjectList();
        List<B> objectBodyList = temp.getObjectBodyList();
        List<R> relationList = temp.getRelationList();
        this.saveObjectData(objectList, objectBodyList, relationList);
        this.afterPersist();
    }

    /**
     * 对 this.getDocTree() 做一些操作
     */
    public void beforePersist() {
    }

    public void afterFinal() {
        this.getObjectTree().clear();
    }

    public List<ObjectNode<D>> obtainObjectData() {
        return this.getObjectTree().getIdMap().values().stream()
                .filter(n -> !StatusEnum.DATABASE.equals(n.getStatus()))
                .toList();
    }

    @AllArgsConstructor
    @Data
    public static class ObjectNodesToEntitiesTemp<O, B, R> {
        private List<O> objectList;
        private List<B> objectBodyList;
        private List<R> relationList;
    }

    public ObjectNodesToEntitiesTemp<O, B, R> nodeConvertToEntities(List<ObjectNode<D>> objectNodes) {
        List<O> objectList = new ArrayList<>(objectNodes.size());
        List<B> objectBodyList = new ArrayList<>(objectNodes.size());
        List<R> relationList = new ArrayList<>(objectNodes.size());
        objectNodes.stream().filter(n -> Objects.nonNull(n.getElement())).forEach(n -> {
            if (log.isDebugEnabled()) {
                log.debug("nodeConvertToEntities, objectNodes:{}", Jsons.str(n.getElement()));
            }
            if (StatusEnum.updateObject(n.getStatus())) {
                objectList.add(this.valueNodeToObjectEntity(n));
            }
            if (StatusEnum.updateObjectBody(n.getStatus())) {
                objectBodyList.add(this.valueNodeToObjectBodyEntity(n));
            }
            if (StatusEnum.updateRelation(n.getStatus())) {
                relationList.addAll(this.data2RelationEntity(n));
            }
        });
        return new ObjectNodesToEntitiesTemp<>(objectList, objectBodyList, relationList);
    }

    public O valueNodeToObjectEntity(ObjectNode<D> node) {
        O entity = this.getObjectHandler().newObjectEntity();
        ObjectElement<D> element = node.getElementOrElseThrow();
        entity.setSpaceId(this.getObjectUidHandler().getSpaceId());
        entity.setDeleted(Boolean.FALSE);
        entity.setObjectId(element.getId());
        entity.setType(element.getType());
        this.extendObjectEntity(entity, element);
        return entity;
    }

    /**
     * @param entity  扩展使用
     * @param element 扩展使用
     */
    public void extendObjectEntity(O entity, ObjectElement<D> element) {
    }

    public B valueNodeToObjectBodyEntity(ObjectNode<D> node) {
        B objectBody = this.getObjectBodyHandler().newObjectBodyEntity();
        ObjectElement<D> element = node.getElementOrElseThrow();
        objectBody.setCreateUser(this.getObjectUidHandler().getUserId());
        objectBody.setCreateTime(LocalDateTime.now());
        objectBody.setUpdateUser(this.getObjectUidHandler().getUserId());
        objectBody.setUpdateTime(LocalDateTime.now());
        objectBody.setObjectId(element.getId());
        objectBody.setName(element.getData().getName());
        objectBody.setValue(Jsons.str(element.getData()));
        this.extendObjectBodyEntity(objectBody, element);
        return objectBody;
    }

    public void extendObjectBodyEntity(B entity, ObjectElement<D> element) {

    }

    public List<R> data2RelationEntity(ObjectNode<D> node) {
        ObjectElement<D> element = node.getElementOrElseThrow();
        return node.findParents().stream().filter(p -> Objects.nonNull(p.getElement())).map(p -> {
            String parentObjectId = p.getElement().getId();
            R relation = this.getRelationHandler().newRelationEntity();
            relation.setCreateUser(this.getObjectUidHandler().getUserId());
            relation.setCreateTime(LocalDateTime.now());
            Map<String, Integer> relationTypeMap = element.getData().getRelationTypeMap();
            if (Objects.nonNull(relationTypeMap) && !relationTypeMap.isEmpty()) {
                relation.setType(relationTypeMap.get(parentObjectId));
            }
            relation.setParentObjectId(parentObjectId);
            relation.setObjectId(element.getId());
            relation.setSorted(element.getData().getSorted());
            this.extendRelationEntity(relation, element);
            return relation;
        }).toList();
    }

    public void extendRelationEntity(R entity, ObjectElement<D> element) {

    }

    public void afterPersist() {
        this.getObjectTree().forEach((i, n) -> n.setStatus(StatusEnum.DATABASE));
    }

    public ObjectNode<D> mergeNode(ObjectNode<D> n, @Nullable ObjectNode<D> old) {
        log.debug("new object id:{}, status:{}", n.getId(), n.getStatus());
        if (Objects.isNull(old)) {
            // 默认CREATED
            n.setStatus(Objects.requireNonNullElse(n.getStatus(), StatusEnum.CREATED));
            return n;
        }
        log.debug("old object id:{}, status:{}", old.getId(), old.getStatus());
        // 现有的node已保存到数据库
        if (StatusEnum.DATABASE.equals(old.getStatus()) || StatusEnum.DATABASE.equals(n.getStatus())) {
            // 如果新的element与数据库的不相同，更新
            if (!this.nodeEquals(n, old)) {
                log.debug("new object not from database and not equal old object.new:{} old:{}", Jsons.str(n), Jsons.str(old));
                ObjectElement<D> newElement = n.getElementOrElseThrow();
                ObjectElement<D> oldElement = old.getElementOrElseThrow();
                newElement.getData().setObjectId(oldElement.getId());
                D d = this.mergeData(newElement.getData(), oldElement.getData());
                oldElement.setData(d);
                old.setStatus(StatusEnum.CACHED_OBJECT_BODY);
            }
            return old;
        }
        // 数据库的objectId
        old.setStatus(StatusEnum.CACHED);
        return old;
    }

    public boolean nodeEquals(ObjectNode<D> n, ObjectNode<D> old) {
        return n.equals(old);
    }

    /**
     * 当数据库已存在相同objectId的记录，但新的数据与数据库不相同时，如何合并数据
     *
     * @param old may be used for further computation in overriding classes
     */
    public D mergeData(D d, D old) {
        return d;
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class FindEntityAndToObjectElementTemp<O, B, R, D extends ObjectBodyData> {
        private D data;

        private @Nullable String objectId;
        private @Nullable B objectBodyEntity;
        private @Nullable O objectEntity;
        private @Nullable ObjectElement<D> objectElement;

        private @Nullable String parentObjectId;
        private @Nullable B parentObjectBodyEntity;
        private @Nullable O parentObjectEntity;
        private @Nullable ObjectElement<D> parentObjectElement;

        private @Nullable List<R> relations;
    }

    /**
     * ObjectElement 的key批量查询
     *
     * @param ds vs
     * @return {@literal Collection<ObjectElement<V>>}
     */
    public List<ObjectElement<D>> findFromDbAndConvertToObjectElement(Collection<D> ds) {
        Assign<FindEntityAndToObjectElementTemp<O, B, R, D>> assign = Assign.build(ds)
                .cast(d -> FindEntityAndToObjectElementTemp.<O, B, R, D>builder().data(d).build())
                .name("get object body")
                // 查询 objectBody
                .addAcquire(this.getObjectBodyHandler()::findObjectBodyByDataIds, this::objectBodyId)
                .name("select ObjectBodyEntities by dataIds")
                .throwException()
                .addAction(t -> this.dataId(t.getData()))
                .name("for biz dataIds")
                .addAssemble((e, b) -> {
                    e.setObjectBodyEntity(b);
                    e.setObjectId(b.getObjectId());
                })
                .backAcquire()
                .addAction(k -> this.parentDataId(k.getData()))
                .name("for biz parentDataIds")
                .addAssemble((e, b) -> {
                    e.setParentObjectBodyEntity(b);
                    e.setParentObjectId(b.getObjectId());
                })
                .backAcquire().backAssign()
                // 获取 object 数据依赖于先获取到 objectBody 数据
                .dependBy()
                .name("get object by objectIds")
                .addAcquire(this.getObjectHandler()::findObjects, O::getObjectId)
                .addAction(FindEntityAndToObjectElementTemp::getObjectId)
                .name("for objectIds")
                .addAssemble((e, o) -> {
                    e.setObjectEntity(o);
                    T type = this.getObjectTypeHandler().getObjectType(o.getType());
                    e.setObjectElement(this.convertToObjectElement(type, e.getObjectBodyEntity(), o.getObjectId(), e.getParentObjectId()));
                })
                .backAcquire()
                .addAction(FindEntityAndToObjectElementTemp::getParentObjectId)
                .name("for parentObjectIds")
                .addAssemble((e, o) -> {
                    e.setParentObjectEntity(o);
                    T type = this.getObjectTypeHandler().getObjectType(o.getType());
                    e.setParentObjectElement(this.convertToObjectElement(type, e.getParentObjectBodyEntity()));
                })
                .backAcquire().backAssign()
                // 获取 relation 数据依赖于先获取到 object 数据
                .dependBy()
                // 根据 objectId 查询 relations ，按objectId 分组，因为一个对象可能有多个父级
                .addAcquireOutGroup(this.getRelationHandler()::findRelationsByObjectIds, R::getObjectId)
                .name("get relation by objectId")
                .addAction(FindEntityAndToObjectElementTemp::getObjectId)
                .addAssemble(FindEntityAndToObjectElementTemp::setRelations)
                .backAcquire().backAssign()
                // 最终执行
                .invoke();
        // 展开，转换为 ObjectElement<V>
        List<FindEntityAndToObjectElementTemp<O, B, R, D>> tempList = assign.toList();
        List<ObjectElement<D>> objectElements = Streams.of(tempList)
                .map(FindEntityAndToObjectElementTemp::getObjectElement).filter(Objects::nonNull).toList();
        Set<String> objectIds = Streams.map(tempList, e -> {
                    List<ObjectElement<D>> elements = new ArrayList<>(this.flatRelations(e.getObjectElement(), e.getRelations()));
                    if (Objects.nonNull(e.getParentObjectElement())) {
                        elements.add(e.getParentObjectElement());
                    }
                    return elements;
                }).flatMap(Collection::stream)
                .map(k -> new String[]{k.getId(), k.getParentId()})
                .flatMap(Stream::of).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> otherParentObjectIds = Streams.map(tempList, k ->
                        Streams.map(k.getRelations(), R::getParentObjectId).toList())
                .flatMap(Collection::stream).filter(Objects::nonNull).filter(objectIds::contains).collect(Collectors.toSet());
        List<ObjectElement<D>> otherElements = Assign.build(otherParentObjectIds)
                .cast(k -> FindEntityAndToObjectElementTemp.<O, B, R, D>builder().objectId(k).build())
                .addAcquire(this.getObjectHandler()::findObjects, O::getObjectId)
                .addAction(FindEntityAndToObjectElementTemp::getObjectId)
                .addAssemble(FindEntityAndToObjectElementTemp::setObjectEntity)
                .backAcquire().backAssign()
                .addAcquire(this.getObjectBodyHandler()::findObjectBodies, B::getObjectId)
                .addAction(FindEntityAndToObjectElementTemp::getObjectId)
                .addAssemble((e, b) -> {
                    e.setObjectBodyEntity(b);
                    if (Objects.nonNull(e.getObjectEntity())) {
                        T type = this.getObjectTypeHandler().getObjectType(e.getObjectEntity().getType());
                        e.setObjectElement(this.convertToObjectElement(type, e.getObjectBodyEntity(), b.getObjectId(), e.getParentObjectId()));
                    }
                })
                .backAcquire().backAssign().invoke()
                .toList().stream().map(FindEntityAndToObjectElementTemp::getObjectElement).toList();
        return Stream.concat(objectElements.stream(), otherElements.stream()).toList();
    }

    public List<ObjectElement<D>> flatRelations(ObjectElement<D> objectElement, List<R> rs) {
        if (Objects.isNull(objectElement)) {
            return List.of();
        }
        if (CollectionUtils.isEmpty(rs)) {
            return List.of(objectElement);
        }
        return Streams.map(rs, r -> {
            @SuppressWarnings("unchecked")
            ObjectElement<D> copy = (ObjectElement<D>) ObjectElementMapper.INSTANCE.copy((ObjectElement<ObjectBodyData>) objectElement);
            this.extendObjectElementWithRelation(copy, r);
            return copy;
        }).toList();
    }

    public void extendObjectElementWithRelation(ObjectElement<D> objectElement, R r) {
    }

    public ObjectElement<D> convertToObjectElement(D data) {
        ObjectElement<D> objectElement = new ObjectElement<>();
        objectElement.setType(this.getObjectTypeHandler().getObjectType(data).getType());
        objectElement.setData(data);
        this.extendObjectElement(objectElement, data);
        return objectElement;
    }

    public void extendObjectElement(ObjectElement<D> objectElement, D data) {
    }

    public ObjectElement<D> convertToObjectElement(T objectType, B entity,
                                                   String objectId, String parentObjectId) {
        ObjectElement<D> objectElement = new ObjectElement<>();
        objectElement.setType(objectType.getType());
        D data = Jsons.obj(entity.getValue(), objectType.getValueClass());
        if (StringUtils.isNotBlank(objectId)) {
            data.setObjectId(objectId);
        }
        if (StringUtils.isNotBlank(parentObjectId)) {
            data.setParentObjectId(parentObjectId);
        }
        objectElement.setData(data);
        return objectElement;
    }

    public ObjectElement<D> convertToObjectElement(T objectType, B entity) {
        return convertToObjectElement(objectType, entity, null, null);
    }

    /**
     * 数据保存处理
     */
    public void saveObjectData(Collection<O> objectList, Collection<B> objectBodyList, Collection<R> relationList) {
        if (!CollectionUtils.isEmpty(objectList)) {
            this.getObjectHandler().saveObjects(objectList);
        }
        if (!CollectionUtils.isEmpty(objectBodyList)) {
            this.getObjectBodyHandler().saveObjectBodies(objectBodyList);
        }
        if (!CollectionUtils.isEmpty(relationList)) {
            this.getRelationHandler().saveRelations(relationList);
        }
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    static class ObjectTemp<O extends ObjectDefiner, R extends RelationDefiner> {
        private String objectId;
        private @Nullable O object;
        private @Nullable List<R> relations;
    }

    protected Function<Collection<String>, Map<String, List<R>>> belongIdRelationsGroupMapping() {
        return ks -> {
            List<R> relations = this.getRelationHandler().findRelationsByParentObjectIds(ks);
            return Streams.groupBy(relations, R::getParentObjectId);
        };
    }

}