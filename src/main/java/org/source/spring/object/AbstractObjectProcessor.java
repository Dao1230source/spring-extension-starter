package org.source.spring.object;

import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.source.spring.exception.BizExceptionEnum;
import org.source.spring.object.entity.ObjectBodyEntityDefiner;
import org.source.spring.object.entity.ObjectEntityDefiner;
import org.source.spring.object.entity.RelationEntityDefiner;
import org.source.spring.object.enums.ObjectTypeDefiner;
import org.source.spring.object.handler.ObjectBodyDbHandlerDefiner;
import org.source.spring.object.handler.ObjectDbHandlerDefiner;
import org.source.spring.object.handler.ObjectTypeHandlerDefiner;
import org.source.spring.object.handler.RelationDbHandlerDefiner;
import org.source.spring.object.mapper.ObjectElementMapper;
import org.source.spring.trace.TraceContext;
import org.source.spring.uid.Ids;
import org.source.utility.assign.Assign;
import org.source.utility.assign.InterruptStrategyEnum;
import org.source.utility.tree.EnhanceTree;
import org.source.utility.tree.define.Node;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Streams;
import org.springframework.aop.framework.AopContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
@Slf4j
public abstract class AbstractObjectProcessor<O extends ObjectEntityDefiner, R extends RelationEntityDefiner,
        B extends ObjectBodyEntityDefiner, V extends AbstractValue, T extends ObjectTypeDefiner, K> {

    protected final TransmittableThreadLocal<EnhanceTree<String, ObjectElement<V>, ObjectNode<String, ObjectElement<V>>>> objectTreeThreadLocal
            = TransmittableThreadLocal.withInitial(() -> EnhanceTree.of(new ObjectNode<String, ObjectElement<V>>()));

    private final ObjectDbHandlerDefiner<O> objectDbHandler;
    private final ObjectBodyDbHandlerDefiner<B, V, K> objectBodyDbHandler;
    private final RelationDbHandlerDefiner<R> relationDbHandler;
    private final ObjectTypeHandlerDefiner<B, V, T> objectTypeHandler;

    @Transactional(rollbackFor = Exception.class)
    public void save(Collection<V> vs) {
        this.merge(vs);
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
                .<ObjectEntityDefiner>cast(ObjectTemp::getObject)
                .addOperates(ObjectEntityDefiner::getType, this.objectTypeHandler.allTypeObjectConsumers());
    }

    public ObjectNode<String, ObjectElement<AbstractValue>> findByObjectIds(Collection<String> objectIds) {
        return this.find(objectIds);
    }

    public EnhanceTree<String, ObjectElement<V>, ObjectNode<String, ObjectElement<V>>> getObjectTree() {
        return objectTreeThreadLocal.get();
    }

    public Function<V, String> getValueIdGetter() {
        return V::getObjectId;
    }

    public Function<B, String> getObjectBodyIdGetter() {
        return B::getObjectId;
    }

    public Function<ObjectElement<V>, String> getElementIdGetter() {
        return ObjectElement::getObjectId;
    }

    public Function<ObjectElement<V>, String> getElementParentIdGetter() {
        return ObjectElement::getParentObjectId;
    }

    /**
     * 转为tree
     *
     * @param vs es
     */
    public void merge(Collection<V> vs) {
        if (log.isDebugEnabled()) {
            log.debug("source values:{}", Jsons.str(vs));
        }
        Collection<V> maybeExistsInDb = this.needValidExistsInDb(vs);
        if (!CollectionUtils.isEmpty(maybeExistsInDb)) {
            if (log.isDebugEnabled()) {
                log.debug("maybeExistsInDb:{}", maybeExistsInDb);
            }
            // 从数据中查询数据并添加到tree中
            List<ObjectElement<V>> dataFromDbList = this.findFromDbAndConvert2FullData(maybeExistsInDb);
            if (log.isDebugEnabled()) {
                log.debug("dataFromDbList:{}", dataFromDbList);
            }
            this.handleDbDataTree().add(dataFromDbList);
        }
        Collection<ObjectElement<V>> objectElements = Streams.map(vs, this::convert2Object).filter(Objects::nonNull).toList();
        this.handleValueDataTree().add(objectElements);
        this.afterTransfer();
    }

    public EnhanceTree<String, ObjectElement<V>, ObjectNode<String, ObjectElement<V>>> handleDbDataTree() {
        EnhanceTree<String, ObjectElement<V>, ObjectNode<String, ObjectElement<V>>> tree = this.getObjectTree();
        tree.setIdGetter(n -> Node.getProperty(n, this.getElementIdGetter()));
        tree.setParentIdGetter(n -> Node.getProperty(n, this.getElementParentIdGetter()));
        tree.setAfterCreateHandler(n -> n.setStatus(StatusEnum.DATABASE));
        tree.setMergeHandler(this::mergeNode);
        return tree;
    }

    public EnhanceTree<String, ObjectElement<V>, ObjectNode<String, ObjectElement<V>>> handleValueDataTree() {
        EnhanceTree<String, ObjectElement<V>, ObjectNode<String, ObjectElement<V>>> tree = this.getObjectTree();
        tree.setAfterCreateHandler(n -> {
            n.setStatus(StatusEnum.CREATED);
            if (Objects.isNull(n.getElement().getObjectId())) {
                n.getElement().setObjectId(Ids.stringId());
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
        List<ObjectElement<V>> objectElements = this.obtainObjectData();
        if (log.isDebugEnabled()) {
            log.debug("ObjectProcessor.save, objectElements:{}", Jsons.str(objectElements));
        }
        List<O> objectList = this.data2ObjectEntities(objectElements);
        List<B> objectBodyList = this.data2ObjectBodyEntities(objectElements);
        List<R> relationList = this.data2RelationEntities(objectElements);
        ((AbstractObjectProcessor<O, R, B, V, T, K>) AopContext.currentProxy()).saveObjectData(objectList, objectBodyList, relationList);
        this.afterPersist();
        this.objectTreeThreadLocal.remove();
    }

    /**
     * 对 this.getDocTree() 做一些操作
     */
    public void beforePersist() {
        this.getObjectTree().forEach((i, n) -> {
            if (StatusEnum.DATABASE.equals(n.getOldStatus())
                    && Objects.nonNull(n.getOldElement())
                    && n.getOldElement().equals(n.getElement())) {
                n.setStatus(StatusEnum.DATABASE);
            }
        });
    }

    public List<ObjectElement<V>> obtainObjectData() {
        return this.getObjectTree().getIdMap().values().stream()
                .filter(n -> !StatusEnum.DATABASE.equals(n.getStatus()))
                .map(n -> {
                    ObjectElement<V> data = n.getElement();
                    if (Objects.isNull(data)) {
                        return null;
                    }
                    data.setStatus(n.getStatus());
                    return data;
                }).filter(Objects::nonNull).toList();
    }

    public void afterPersist() {
        this.getObjectTree().forEach((i, n) -> n.setStatus(StatusEnum.DATABASE));
    }

    public ObjectNode<String, ObjectElement<V>> mergeNode(ObjectNode<String, ObjectElement<V>> n, ObjectNode<String, ObjectElement<V>> old) {
        log.debug("new object id:{}, status:{}", n.getId(), n.getStatus());
        if (Objects.isNull(old)) {
            // 默认CREATED
            n.setStatus(Objects.requireNonNullElse(n.getStatus(), StatusEnum.CREATED));
            return n;
        }
        log.debug("old object id:{}, status:{}", old.getId(), old.getStatus());
        // 现有的node已保存到数据库
        if (StatusEnum.DATABASE.equals(old.getStatus())) {
            // 如果新的element与数据库的不相同，更新
            if (!StatusEnum.DATABASE.equals(n.getStatus()) && !this.nodeEquals(n, old)) {
                log.debug("new object not from database and not equal old object.\nnew:{} \nold:{}", Jsons.str(n), Jsons.str(old));
                old.setOldElement(old.getElement());
                old.setOldStatus(old.getStatus());
                n.getElement().setObjectId(old.getOldElement().getObjectId());
                old.setElement(this.mergeValue(n.getElement(), old.getElement()));
                old.setStatus(StatusEnum.CACHED_OBJECT_BODY);
            }
            return old;
        }
        // 数据库的objectId
        if (StatusEnum.DATABASE.equals(n.getStatus())) {
            old.getElement().setObjectId(n.getElement().getObjectId());
        }
        old.setStatus(StatusEnum.CACHED);
        return old;
    }

    public boolean nodeEquals(ObjectNode<String, ObjectElement<V>> n, ObjectNode<String, ObjectElement<V>> old) {
        return n.equals(old);
    }

    /**
     * 当数据库已存在相同objectId的记录，但新的数据与数据库不相同时，如何合并数据
     *
     * @param old may be used for further computation in overriding classes
     */
    public ObjectElement<V> mergeValue(ObjectElement<V> n, ObjectElement<V> old) {
        return n;
    }

    /**
     * 需要校验是否在数据库是否存在的V
     *
     * @param vs vs
     * @return vs
     */
    public Collection<V> needValidExistsInDb(Collection<V> vs) {
        Map<String, ObjectNode<String, ObjectElement<V>>> idMap = this.getObjectTree().getIdMap();
        return Streams.retain(vs, v -> {
            ObjectNode<String, ObjectElement<V>> node = idMap.get(this.getValueIdGetter().apply(v));
            return Objects.isNull(node) || !StatusEnum.DATABASE.equals(node.getStatus());
        }).toList();
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class FindEntityAndToFullDataTemp<O, B, R, V extends AbstractValue> {
        private V value;
        private B objectBodyEntity;
        private ObjectElement<V> fullData;
        private List<R> relations;

        private B parentObjectBodyEntity;
        private O parentObjectEntity;
        private ObjectElement<V> parentFullData;
    }

    /**
     * ObjectElement 的key批量查询
     *
     * @param vs vs
     * @return {@literal Collection<ObjectElement>}
     */
    public List<ObjectElement<V>> findFromDbAndConvert2FullData(Collection<V> vs) {
        Map<String, V> valueTypeMap = Streams.toMap(vs, this.getValueIdGetter());
        return Assign.build(vs)
                .cast(v -> FindEntityAndToFullDataTemp.<O, B, R, V>builder().value(v).build())
                .name("get objectBody and parent objectBody")
                // 查询 objectBody
                .addAcquire(this.objectBodyDbHandler::findObjectBodiesByKeys, this.objectBodyDbHandler::objectBodyToKey)
                .throwException()
                .afterProcessor((e, map) ->
                        this.handlerAfterObjectBodyConvertToFullData(e.getFullData(), map))
                .addAction(k -> this.objectBodyDbHandler.valueToKey(k.getValue()))
                // 给 objectBodyEntity、ObjectElement<V > 赋值
                .addAssemble((e, b) -> {
                    e.setObjectBodyEntity(b);
                    String objectBodyId = this.getObjectBodyIdGetter().apply(b);
                    V v = valueTypeMap.get(objectBodyId);
                    BizExceptionEnum.OBJECT_CANNOT_FIND_VALUE.nonNull(v, objectBodyId);
                    ObjectTypeDefiner type = this.objectTypeHandler.getObjectType(v);
                    e.setFullData(this.objectTypeHandler.convert2FullData(type, b));
                })
                .backAcquire()
                .addAction(k -> this.objectBodyDbHandler.valueToParentKey(k.getValue()))
                .addAssemble(FindEntityAndToFullDataTemp::setParentObjectBodyEntity)
                .backAcquire().backAssign()
                .addBranch(e -> Objects.nonNull(e.getParentObjectBodyEntity()))
                .name("get parent fullData")
                .addAcquire(this.objectDbHandler::findObjects, O::getObjectId)
                .addAction(e -> e.getParentObjectBodyEntity().getObjectId())
                .addAssemble((e, o) -> {
                    e.setParentObjectEntity(o);
                    ObjectTypeDefiner type = this.objectTypeHandler.getObjectType(o.getType());
                    e.setParentFullData(this.objectTypeHandler.convert2FullData(type, e.getParentObjectBodyEntity()));
                })
                .backAcquire().backAssign()
                .backSuper()
                // 这里新建分支是因为需要先查询 objectBody，有依赖关系
                .addBranch(e -> Objects.nonNull(e.getObjectBodyEntity()))
                .name("get relations")
                // 根据 objectId 查询 relations ，按objectId 分组，因为一个对象可能有多个父级
                .<String, List<R>>addAcquire(ks -> Streams.of(this.relationDbHandler.findRelationsByObjectIds(ks)).collect(Collectors.groupingBy(R::getObjectId)))
                .addAction(e -> e.getObjectBodyEntity().getObjectId())
                .addAssemble(FindEntityAndToFullDataTemp::setRelations)
                .backAcquire().backAssign()
                .backSuper()
                // 最终执行
                .invoke()
                // 展开，转换为 ObjectElement<V >
                .casts(es -> Streams.map(es, e -> {
                    List<ObjectElement<V>> fullData = new ArrayList<>(this.processFullData(e.getFullData(), e.getRelations()));
                    if (Objects.nonNull(e.getParentFullData())) {
                        fullData.add(e.getParentFullData());
                    }
                    return fullData;
                }).flatMap(Collection::stream).toList())
                .toList();
    }

    /**
     * object 转换为 ObjectElement<V > 时做一些特殊处理
     *
     * @param fullData  fullData
     * @param objectMap {@literal <K, ObjectBodyEntity >}
     */
    public void handlerAfterObjectBodyConvertToFullData(ObjectElement<V> fullData, Map<K, B> objectMap) {

    }

    public List<ObjectElement<V>> processFullData(ObjectElement<V> objectElement, List<R> rs) {
        if (Objects.isNull(objectElement)) {
            return List.of();
        }
        if (CollectionUtils.isEmpty(rs)) {
            return List.of(objectElement);
        } else {
            return Streams.map(rs, r -> {
                @SuppressWarnings("unchecked")
                ObjectElement<V> copy = (ObjectElement<V>) ObjectElementMapper.INSTANCE.copy((ObjectElement<AbstractValue>) objectElement);
                this.fullDataFillRelation(copy, r);
                return copy;
            }).toList();
        }
    }

    public void fullDataFillRelation(ObjectElement<V> fullData, R r) {
        fullData.setParentObjectId(r.getParentObjectId());
        fullData.setRelationType(r.getType());
        fullData.setBelongId(r.getBelongId());
    }

    public ObjectElement<V> convert2Object(V objectValue) {
        ObjectElement<V> objectElement = new ObjectElement<>();
        objectElement.setObjectId(objectValue.getObjectId());
        objectElement.setType(this.objectTypeHandler.getObjectType(objectValue).getType());
        objectElement.setValue(objectValue);
        return objectElement;
    }

    /**
     * 数据保存处理
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveObjectData(List<O> objectList, List<B> objectBodyList, List<R> relationList) {
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

    public List<O> data2ObjectEntities(Collection<ObjectElement<V>> objectData) {
        return objectData.stream().filter(k -> StatusEnum.updateObject(k.getStatus()))
                .map(this::data2ObjectEntity).toList();
    }

    public O data2ObjectEntity(ObjectElement<V> data) {
        O entity = this.objectDbHandler.newObjectEntity();
        BizExceptionEnum.OBJECT_NEW_OBJECT_ENTITY_NONNULL.nonNull(entity, data.getValue().getObjectId());
        entity.setObjectId(data.getObjectId());
        entity.setType(data.getType());
        entity.setSpaceId(TraceContext.getSpaceIdOrDefault());
        entity.setDeleted(Boolean.FALSE);
        entity.setCreateUser(TraceContext.getUserIdOrDefault());
        return entity;
    }

    public List<B> data2ObjectBodyEntities(Collection<ObjectElement<V>> objectData) {
        return objectData.stream().filter(k -> StatusEnum.updateObjectBody(k.getStatus()))
                .map(this::data2ObjectBodyEntity).toList();
    }

    public B data2ObjectBodyEntity(ObjectElement<V> data) {
        B entity = this.objectBodyDbHandler.newObjectBodyEntity();
        BizExceptionEnum.OBJECT_NEW_OBJECT_BODY_ENTITY_NONNULL.nonNull(entity, data.getValue().getObjectId());
        entity.setObjectId(data.getObjectId());
        entity.setName(data.getName());
        entity.setValue(Jsons.str(data.getValue()));
        entity.setCreateUser(TraceContext.getUserIdOrDefault());
        entity.setUpdateUser(TraceContext.getUserIdOrDefault());
        return entity;
    }

    public List<R> data2RelationEntities(Collection<ObjectElement<V>> objectData) {
        return objectData.stream()
                .filter(k -> StatusEnum.updateRelation(k.getStatus()))
                .filter(k -> Objects.nonNull(k.getParentObjectId()))
                .map(this::data2RelationEntity).toList();
    }

    public R data2RelationEntity(ObjectElement<V> data) {
        R entity = this.relationDbHandler.newRelationEntity();
        BizExceptionEnum.OBJECT_NEW_RELATION_ENTITY_NONNULL.nonNull(entity, data.getValue().getObjectId());
        entity.setObjectId(data.getObjectId());
        entity.setParentObjectId(data.getParentObjectId());
        entity.setType(data.getRelationType());
        entity.setBelongId(data.getBelongId());
        entity.setCreateUser(TraceContext.getUserIdOrDefault());
        return entity;
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    static class ObjectTemp<O extends ObjectEntityDefiner, R extends RelationEntityDefiner> {
        private String objectId;
        private O object;
        private List<R> relations;
    }

    protected ObjectNode<String, ObjectElement<AbstractValue>> find(Collection<String> objectIds) {
        Collection<ObjectElement<AbstractValue>> fullData = Assign.build(objectIds)
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
                // ObjectTemp 转为 ObjectElement<AbstractValue >
                .casts(es -> Streams.map(es, k -> {
                    if (Objects.isNull(k.getObject())) {
                        return List.<ObjectElement<AbstractValue>>of();
                    }
                    ObjectElement<AbstractValue> data = new ObjectElement<>();
                    O object = k.getObject();
                    data.setSpaceId(object.getSpaceId());
                    data.setType(object.getType());
                    data.setObjectId(k.getObjectId());
                    if (Objects.isNull(k.getRelations())) {
                        return List.of(data);
                    }
                    return Streams.map(k.getRelations(), r -> {
                        ObjectElement<AbstractValue> copy = ObjectElementMapper.INSTANCE.copy(data);
                        copy.setObjectId(r.getObjectId());
                        copy.setParentObjectId(r.getParentObjectId());
                        copy.setBelongId(r.getBelongId());
                        copy.setRelationType(r.getType());
                        return copy;
                    }).collect(Collectors.toList());
                }).flatMap(Collection::stream).toList())
                .parallel().interruptStrategy(InterruptStrategyEnum.ANY)
                // 查询 objectBody
                .addBranches(ObjectElement::getType, this.objectTypeHandler.allTypeAssigners()).invoke()
                .toList();
        // 组成 tree 结构
        return EnhanceTree.of(new ObjectNode<String, ObjectElement<AbstractValue>>()).add(fullData);
    }

    protected Function<Collection<String>, Map<String, List<R>>> belongIdRelationsGroupMapping() {
        return ks -> {
            List<R> relations = this.relationDbHandler.findRelationsByParentObjectIds(ks);
            List<R> relationsByBelongIds = this.relationDbHandler.findRelationsByBelongIds(ks);
            Set<R> rs = new HashSet<>(relations);
            rs.addAll(relationsByBelongIds);
            rs.forEach(r -> {
                if (Objects.isNull(r.getBelongId())) {
                    r.setBelongId(r.getObjectId());
                }
            });
            return Streams.groupBy(rs, R::getBelongId);
        };
    }

}