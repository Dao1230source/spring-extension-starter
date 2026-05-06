package org.source.spring.object.definer.processor;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.source.spring.common.exception.SpExtExceptionEnum;
import org.source.spring.object.JsonUtil;
import org.source.spring.object.ObjectBodyData;
import org.source.spring.object.ObjectElement;
import org.source.spring.object.ObjectNode;
import org.source.spring.object.definer.entity.ObjectBodyEntityDefiner;
import org.source.spring.object.definer.entity.ObjectEntityDefiner;
import org.source.spring.object.definer.entity.RelationEntityDefiner;
import org.source.spring.object.definer.enums.ObjectTypeDefiner;
import org.source.spring.object.definer.handler.ObjectBodyDbHandlerDefiner;
import org.source.spring.object.definer.handler.ObjectDbHandlerDefiner;
import org.source.spring.object.definer.handler.ObjectTypeHandlerDefiner;
import org.source.spring.object.definer.handler.RelationDbHandlerDefiner;
import org.source.spring.object.enums.StatusEnum;
import org.source.spring.object.mapper.ObjectElementMapper;
import org.source.spring.trace.TraceContext;
import org.source.spring.uid.Uids;
import org.source.utility.assign.Assign;
import org.source.utility.assign.InterruptStrategyEnum;
import org.source.utility.tree.EnhanceTree;
import org.source.utility.tree.define.IdExtend;
import org.source.utility.tree.define.Node;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Streams;
import org.springframework.aop.framework.AopContext;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

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
 * @param <P> 处理器
 */
@AllArgsConstructor
@Getter
@Slf4j
public abstract class AbstractObjectProcessor<
        O extends ObjectEntityDefiner, R extends RelationEntityDefiner, B extends ObjectBodyEntityDefiner, D extends ObjectBodyData,
        T extends ObjectTypeDefiner<O, R, B, D, T, P>,
        P extends AbstractObjectProcessor<O, R, B, D, T, P>> {

    protected final EnhanceTree<String, ObjectElement<D>, ObjectNode<D>> objectTree = EnhanceTree.of(new ObjectNode<>());
    protected final IdExtend<String, ObjectElement<D>, ObjectNode<D>> treeIdExtendData = new IdExtend<>("data",
            n -> Node.getProperty(n, e -> this.dataId(e.getData())),
            n -> Node.getProperty(n, e -> this.dataParentId(e.getData())));

    private final ObjectDbHandlerDefiner<O> objectDbHandler;
    private final ObjectBodyDbHandlerDefiner<B> objectBodyDbHandler;
    private final RelationDbHandlerDefiner<R> relationDbHandler;
    private final ObjectTypeHandlerDefiner<O, R, B, D, T, P> objectTypeHandler;

    @Transactional(rollbackFor = Exception.class)
    public void save(Collection<D> ds) {
        this.merge(ds);
        this.save();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Collection<String> objectIds) {
        this.objectDbHandler.deleteObjects(objectIds);
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Collection<String> objectIds) {
        Assign.build(objectIds)
                .cast(e -> ObjectTemp.<O, R>builder().objectId(e).build())
                .parallel().interruptStrategy(InterruptStrategyEnum.ANY)
                // 查询 object
                .addAcquire(this.objectDbHandler::findObjects, O::getObjectId)
                .throwException()
                .addAction(ObjectTemp::getObjectId)
                .addAssemble(ObjectTemp::setObject)
                .backAcquire().backAssign().invoke()
                .cast(ObjectTemp::getObject)
                .addOperates(ObjectEntityDefiner::getType, this.objectTypeHandler.typeObjectOperateMap());
    }

    public ObjectNode<D> findByObjectIds(Collection<String> objectIds) {
        return this.find(objectIds);
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

    public @Nullable String dataParentId(@Nullable D d) {
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
            Collection<D> maybeExistsInDb = this.needValidExistsInDb(ds);
            if (!CollectionUtils.isEmpty(maybeExistsInDb)) {
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
            Collection<ObjectElement<D>> objectElements = Streams.map(ds, this::convertToObjectElement).filter(Objects::nonNull).toList();
            this.handleValueDataTree().add(objectElements);
            this.afterTransfer();
        } catch (Exception e) {
            this.getObjectTree().clear();
            log.error("AbstractObjectProcessor.merge error", e);
            SpExtExceptionEnum.OBJECT_MERGE_ERROR.throwException(e);
        }
    }

    /**
     * 需要校验是否在数据库是否存在的V
     *
     * @param ds vs
     * @return vs
     */
    public Collection<D> needValidExistsInDb(Collection<D> ds) {
        Map<String, ObjectNode<D>> idMap = this.getObjectTree().getIdMap();
        return Streams.retain(ds, d -> {
            String valueId = this.dataId(d);
            if (StringUtils.isEmpty(valueId)) {
                return false;
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
                Node.setProperty(n, e -> e.getData().setObjectId(Uids.stringId()));
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
    @SuppressWarnings("unchecked")
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
        ((AbstractObjectProcessor<O, R, B, D, T, P>) AopContext.currentProxy()).saveObjectData(objectList, objectBodyList, relationList);
        this.afterPersist();
    }

    /**
     * 对 this.getDocTree() 做一些操作
     */
    public void beforePersist() {
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
        O entity = this.objectDbHandler.newObjectEntity();
        ObjectElement<D> element = node.getElementOrElseThrow();
        entity.setSpaceId(TraceContext.getSpaceIdOrDefault());
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
        B entity = this.objectBodyDbHandler.newObjectBodyEntity();
        ObjectElement<D> element = node.getElementOrElseThrow();
        entity.setCreateUser(TraceContext.getUserIdOrDefault());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateUser(TraceContext.getUserIdOrDefault());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setObjectId(element.getId());
        entity.setValue(JsonUtil.str(element.getData()));
        this.extendObjectBodyEntity(entity, element);
        return entity;
    }

    public void extendObjectBodyEntity(B entity, ObjectElement<D> element) {

    }

    public List<R> data2RelationEntity(ObjectNode<D> node) {
        ObjectElement<D> element = node.getElementOrElseThrow();
        Map<String, Integer> parentIdRelationTypeMap = node.getParentIdRelationTypeMap();
        return node.findParents().stream().filter(p -> Objects.nonNull(p.getElement())).map(p -> {
            String parentObjectId = p.getElement().getId();
            R entity = this.relationDbHandler.newRelationEntity();
            entity.setCreateUser(TraceContext.getUserIdOrDefault());
            entity.setCreateTime(LocalDateTime.now());
            entity.setType(parentIdRelationTypeMap.getOrDefault(parentObjectId, element.getData().getRelationType()));
            entity.setParentObjectId(parentObjectId);
            entity.setObjectId(element.getId());
            entity.setSorted(element.getData().getSorted());
            this.extendRelationEntity(entity, element);
            return entity;
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
                .name("get_object_data")
                // 查询 objectBody
                .addAcquire(this.objectBodyDbHandler::findObjectBodiesByKeys, this::objectBodyId)
                .name("get_object_body_by_dataId_and_parentDataId")
                .throwException()
                // ObjectBody key
                .addAction(t -> this.dataId(t.getData()))
                .addAssemble((e, b) -> {
                    e.setObjectBodyEntity(b);
                    e.setObjectId(b.getObjectId());
                })
                .backAcquire()
                // ObjectBodyEntity parentKey
                .addAction(k -> this.dataParentId(k.getData()))
                .addAssemble((e, b) -> {
                    e.setParentObjectBodyEntity(b);
                    e.setParentObjectId(b.getObjectId());
                })
                .backAcquire().backAssign().invoke()
                .addAcquire(this.objectDbHandler::findObjects, O::getObjectId)
                .name("get_object_by_objectId_and_parentObjectId")
                .addAction(FindEntityAndToObjectElementTemp::getObjectId)
                .addAssemble((e, o) -> {
                    e.setObjectEntity(o);
                    T type = this.objectTypeHandler.getObjectType(o.getType());
                    ObjectElement<D> objectElement = this.convertToObjectElement(type, e.getObjectBodyEntity());
                    objectElement.getData().setObjectId(o.getObjectId());
                    objectElement.getData().setParentObjectId(e.getParentObjectId());
                    e.setObjectElement(objectElement);
                })
                .backAcquire()
                .addAction(FindEntityAndToObjectElementTemp::getParentObjectId)
                .addAssemble((e, o) -> {
                    e.setParentObjectEntity(o);
                    T type = this.objectTypeHandler.getObjectType(o.getType());
                    e.setParentObjectElement(this.convertToObjectElement(type, e.getParentObjectBodyEntity()));
                })
                .backAcquire().backAssign()
                // 根据 objectId 查询 relations ，按objectId 分组，因为一个对象可能有多个父级
                .addAcquireOutGroup(this.relationDbHandler::findRelationsByObjectIds, R::getObjectId)
                .name("get_relation_by_objectId")
                .addAction(FindEntityAndToObjectElementTemp::getObjectId)
                .addAssemble(FindEntityAndToObjectElementTemp::setRelations)
                .backAcquire().backAssign()
                .backSuper()
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
                .addAcquire(this.objectDbHandler::findObjects, O::getObjectId)
                .addAction(FindEntityAndToObjectElementTemp::getObjectId)
                .addAssemble(FindEntityAndToObjectElementTemp::setObjectEntity)
                .backAcquire().backAssign()
                .addAcquire(this.objectBodyDbHandler::findObjectBodies, B::getObjectId)
                .addAction(FindEntityAndToObjectElementTemp::getObjectId)
                .addAssemble((e, b) -> {
                    e.setObjectBodyEntity(b);
                    T type = this.objectTypeHandler.getObjectType(e.getObjectEntity().getType());
                    e.setObjectElement(this.convertToObjectElement(type, e.getObjectBodyEntity()));
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
        objectElement.setType(this.objectTypeHandler.getObjectType(data).getType());
        objectElement.setData(data);
        this.extendObjectElement(objectElement, data);
        return objectElement;
    }

    public void extendObjectElement(ObjectElement<D> objectElement, D data) {
    }

    public ObjectElement<D> convertToObjectElement(T objectType, B entity) {
        ObjectElement<D> objectElement = new ObjectElement<>();
        objectElement.setType(objectType.getType());
        D data = this.objectTypeHandler.convertToData(objectType, entity);
        objectElement.setData(data);
        return objectElement;
    }

    /**
     * 数据保存处理
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveObjectData(Collection<O> objectList, Collection<B> objectBodyList, Collection<R> relationList) {
        if (!CollectionUtils.isEmpty(objectList)) {
            this.objectDbHandler.saveObjects(objectList);
        }
        if (!CollectionUtils.isEmpty(objectBodyList)) {
            this.objectBodyDbHandler.saveObjectBodies(objectBodyList);
        }
        if (!CollectionUtils.isEmpty(relationList)) {
            this.relationDbHandler.saveRelations(relationList);
        }
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    static class ObjectTemp<O extends ObjectEntityDefiner, R extends RelationEntityDefiner> {
        private String objectId;
        private @Nullable O object;
        private @Nullable List<R> relations;
    }

    protected ObjectNode<D> find(Collection<String> objectIds) {
        Collection<ObjectElement<D>> fullData = Assign.build(objectIds)
                // objectId 转为 ObjectTemp
                .cast(e -> ObjectTemp.<O, R>builder().objectId(e).build())
                .parallel().interruptStrategy(InterruptStrategyEnum.ANY)
                // 查询 object
                .addAcquire(this.objectDbHandler::findObjects, O::getObjectId)
                .throwException()
                .addAction(ObjectTemp::getObjectId)
                .addAssemble(ObjectTemp::setObject)
                .backAcquire().backAssign()
                // 查询 relation 并按 belongId 分组
                .addAcquire(this.belongIdRelationsGroupMapping())
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
                .addBranches(ObjectElement::getType, this.objectTypeHandler.typeAssignerMap()).invoke()
                .toList();
        // 组成 tree 结构
        return EnhanceTree.of(new ObjectNode<D>()).add(fullData);
    }

    protected Function<Set<String>, Map<String, List<R>>> belongIdRelationsGroupMapping() {
        return ks -> {
            List<R> relations = this.relationDbHandler.findRelationsByParentObjectIds(ks);
            return Streams.groupBy(relations, R::getParentObjectId);
        };
    }

}