package org.source.spring.object.processor;

import com.alibaba.ttl.TransmittableThreadLocal;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.source.spring.exception.BizExceptionEnum;
import org.source.spring.object.AbstractValue;
import org.source.spring.object.StatusEnum;
import org.source.spring.object.data.ObjectFullData;
import org.source.spring.object.entity.ObjectBodyEntityIdentity;
import org.source.spring.object.entity.ObjectEntityIdentity;
import org.source.spring.object.entity.RelationEntityIdentity;
import org.source.spring.object.enums.ObjectTypeIdentity;
import org.source.spring.object.mapper.ObjectFullDataMapper;
import org.source.spring.object.tree.ObjectNode;
import org.source.spring.trace.TraceContext;
import org.source.spring.uid.Ids;
import org.source.utility.assign.Assign;
import org.source.utility.assign.InterruptStrategyEnum;
import org.source.utility.tree.Tree;
import org.source.utility.tree.identity.AbstractNode;
import org.source.utility.tree.identity.Node;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Streams;
import org.springframework.aop.framework.AopContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Slf4j
public abstract class AbstractObjectProcessor<O extends ObjectEntityIdentity, R extends RelationEntityIdentity,
        B extends ObjectBodyEntityIdentity,
        V extends AbstractValue, T extends ObjectTypeIdentity, K> {

    protected final TransmittableThreadLocal<Tree<String, ObjectFullData<V>, ObjectNode<String, ObjectFullData<V>>>> objectTreeThreadLocal
            = TransmittableThreadLocal.withInitial(ObjectNode::buildTree);

    /**
     * object
     */
    public abstract O newObjectEntity();

    public abstract List<O> findObjects(@NotEmpty Collection<String> objectIds);

    public abstract void saveObjects(@NotEmpty Collection<O> objects);

    /**
     * relation
     */
    public abstract R newRelationEntity();

    public abstract List<R> findRelationsByObjectIds(@NotEmpty Collection<String> objectIds);

    public abstract List<R> findRelationsByParentObjectIds(@NotEmpty Collection<String> parentObjectIds);

    public abstract List<R> findRelationsByBelongIds(@NotEmpty Collection<String> belongIds);

    public abstract void saveRelations(@NotEmpty Collection<R> relations);

    /**
     * object body
     */
    public abstract B newObjectBodyEntity();

    public abstract List<B> findObjectBodies(@NotEmpty Collection<String> objectIds);

    /**
     * 通常 object body 的唯一键是 objectId，但实际业务新增时中可能会使用其他唯一键来查询是否已存在数据
     *
     * @param ks ks
     * @return list
     */
    public abstract List<B> findObjectBodiesByKeys(@NotEmpty Collection<K> ks);

    public abstract K objectBodyToKey(B b);

    public abstract K valueToKey(V v);

    public abstract void saveObjectBodies(@NotEmpty Collection<B> objectBodies);

    public abstract Map<Integer, AbstractObjectProcessor<ObjectEntityIdentity, RelationEntityIdentity,
            ObjectBodyEntityIdentity, AbstractValue, ObjectTypeIdentity, Object>> objectType2ProcessorMap();

    public abstract Map<Integer, T> type2ObjectTypeMap();

    public abstract Map<Class<? extends V>, T> class2ObjectTypeMap();

    public Tree<String, ObjectFullData<V>, ObjectNode<String, ObjectFullData<V>>> getObjectTree() {
        return objectTreeThreadLocal.get();
    }

    public Function<V, String> getValueIdGetter() {
        return V::getObjectId;
    }

    public Function<B, String> getObjectBodyIdGetter() {
        return B::getObjectId;
    }

    public Function<ObjectFullData<V>, String> getFullDataIdGetter() {
        return ObjectFullData::getObjectId;
    }

    public Function<ObjectFullData<V>, String> getFullDataParentIdGetter() {
        return ObjectFullData::getParentObjectId;
    }

    /**
     * obtain type for object value
     */
    public ObjectTypeIdentity getObjectType(V v) {
        ObjectTypeIdentity type = this.class2ObjectTypeMap().get(v.getClass());
        if (Objects.isNull(type)) {
            throw BizExceptionEnum.OBJECT_VALUE_CLASS_NOT_DEFINED.except("class:{}", v.getClass());
        }
        return type;
    }

    /**
     * convert entity to value
     */
    public ObjectTypeIdentity getObjectType(Integer type) {
        ObjectTypeIdentity objectType = this.type2ObjectTypeMap().get(type);
        if (Objects.isNull(objectType)) {
            throw BizExceptionEnum.OBJECT_TYPE_NOT_DEFINED.except("type:{}", type);
        }
        return objectType;
    }

    public V convert2Value(ObjectTypeIdentity objectType, ObjectBodyEntityIdentity objectBodyEntity) {
        return Jsons.obj(objectBodyEntity.getValue(), objectType.getValueClass());
    }

    public ObjectFullData<V> convert2FullData(ObjectTypeIdentity objectType, B entity) {
        ObjectFullData<V> fullData = new ObjectFullData<>();
        fullData.setType(objectType.getType());
        fullData.setName(entity.getName());
        fullData.setObjectId(entity.getObjectId());
        V value = this.convert2Value(objectType, entity);
        fullData.setValue(value);
        return fullData;
    }

    /**
     * 转为tree
     *
     * @param vs es
     */
    public void transfer2tree(Collection<V> vs) {
        if (log.isDebugEnabled()) {
            log.debug("source values:{}", Jsons.str(vs));
        }
        Collection<V> maybeExistsDb = this.maybeExistsDb(vs);
        if (!CollectionUtils.isEmpty(maybeExistsDb)) {
            if (log.isDebugEnabled()) {
                log.debug("maybeExistsDb:{}", maybeExistsDb);
            }
            // 从数据中查询数据并添加到tree中
            List<ObjectFullData<V>> dataFromDbList = this.findFromDbAndConvert2FullData(maybeExistsDb).stream().toList();
            if (log.isDebugEnabled()) {
                log.debug("dataFromDbList:{}", dataFromDbList);
            }
            this.handleDbDataTree().add(dataFromDbList);
        }
        Collection<ObjectFullData<V>> objectFullData = Streams.map(vs, this::convert2Object).filter(Objects::nonNull).toList();
        this.handleValueDataTree().add(objectFullData);
        this.afterTransfer();
    }

    public Tree<String, ObjectFullData<V>, ObjectNode<String, ObjectFullData<V>>> handleDbDataTree() {
        Tree<String, ObjectFullData<V>, ObjectNode<String, ObjectFullData<V>>> tree = this.getObjectTree();
        tree.setIdGetter(n -> Node.getProperty(n, this.getFullDataIdGetter()));
        tree.setParentIdGetter(n -> Node.getProperty(n, this.getFullDataParentIdGetter()));
        tree.setKeepOldIndex(true);
        tree.setAfterCreateHandler(n -> n.setStatus(StatusEnum.DATABASE));
        tree.setMergeHandler(this::mergeNode);
        return tree;
    }

    public Tree<String, ObjectFullData<V>, ObjectNode<String, ObjectFullData<V>>> handleValueDataTree() {
        Tree<String, ObjectFullData<V>, ObjectNode<String, ObjectFullData<V>>> tree = this.getObjectTree();
        tree.setKeepOldIndex(true);
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
    public void persist2Database() {
        this.beforePersist();
        List<ObjectFullData<V>> objectFullData = this.obtainObjectData();
        if (log.isDebugEnabled()) {
            log.debug("ObjectProcessor.save, objectDataList:{}", Jsons.str(objectFullData));
        }
        List<O> objectList = this.data2ObjectEntities(objectFullData);
        List<B> objectBodyList = this.data2ObjectBodyEntities(objectFullData);
        List<R> relationList = this.data2RelationEntities(objectFullData);
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

    public List<ObjectFullData<V>> obtainObjectData() {
        return this.getObjectTree().getIdMap().values().stream()
                .filter(n -> !StatusEnum.DATABASE.equals(n.getStatus()))
                .map(n -> {
                    ObjectFullData<V> data = n.getElement();
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

    public ObjectNode<String, ObjectFullData<V>> mergeNode(ObjectNode<String, ObjectFullData<V>> n, ObjectNode<String, ObjectFullData<V>> old) {
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
                old.setStatus(StatusEnum.CACHED);
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

    public boolean nodeEquals(ObjectNode<String, ObjectFullData<V>> n, ObjectNode<String, ObjectFullData<V>> old) {
        return n.equals(old);
    }

    /**
     * 当数据库已存在相同objectId的记录，但新的数据与数据库不相同时，如何合并数据
     *
     * @param old may be used for further computation in overriding classes
     */
    public ObjectFullData<V> mergeValue(ObjectFullData<V> n, ObjectFullData<V> old) {
        return n;
    }

    public Collection<V> maybeExistsDb(Collection<V> vs) {
        List<V> vsFromDb = this.getObjectTree().find(n -> StatusEnum.DATABASE.equals(n.getStatus()))
                .stream().map(AbstractNode::getElement).filter(Objects::nonNull).map(ObjectFullData::getValue).toList();
        return Streams.notRetain(vs, AbstractValue::getId, vsFromDb).toList();
    }

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class FindEntityAndToFullDataTemp<V extends AbstractValue, B, R> {
        private V value;
        private B objectBodyEntity;
        private ObjectFullData<V> fullData;
        private List<R> relations;
    }

    /**
     * ObjectFullData 的key批量查询
     *
     * @param vs vs
     * @return {@literal Collection<ObjectFullData>}
     */
    public Collection<ObjectFullData<V>> findFromDbAndConvert2FullData(Collection<V> vs) {
        Map<String, V> valueTypeMap = Streams.toMap(vs, this.getValueIdGetter());
        return Assign.build(vs)
                .cast(v -> FindEntityAndToFullDataTemp.<V, B, R>builder().value(v).build())
                // 查询 objectBody
                .addAcquire(this::findObjectBodiesByKeys, this::objectBodyToKey)
                .throwException()
                .afterProcessor((e, map) ->
                        this.handlerAfterObjectBodyConvertToFullData(e.getFullData(), map))
                .addAction(k -> this.valueToKey(k.getValue()))
                // 给 objectBodyEntity、ObjectFullData<V> 赋值
                .addAssemble((e, b) -> {
                    e.setObjectBodyEntity(b);
                    String objectBodyId = this.getObjectBodyIdGetter().apply(b);
                    V v = valueTypeMap.get(objectBodyId);
                    BizExceptionEnum.OBJECT_CANNOT_FIND_VALUE.nonNull(v, objectBodyId);
                    ObjectTypeIdentity type = this.getObjectType(v);
                    ObjectFullData<V> fullData = this.convert2FullData(type, b);
                    e.setFullData(fullData);
                })
                .backAcquire().backAssign()
                // 这里新建分支是因为需要先查询 objectBody，有依赖关系
                .addBranch()
                // 根据 objectId 查询 relations ，按objectId 分组，因为一个对象可能有多个父级
                .<String, List<R>>addAcquire(ks -> Streams.of(this.findRelationsByObjectIds(ks)).collect(Collectors.groupingBy(R::getObjectId)))
                .addAction(e -> e.getObjectBodyEntity().getObjectId())
                .addAssemble(FindEntityAndToFullDataTemp::setRelations)
                .backAcquire().backAssign().invoke()
                // 展开，转换为 ObjectFullData<V>
                .casts(es -> Streams.map(es, e -> {
                    List<R> rs = e.getRelations();
                    ObjectFullData<V> objectFullData = e.getFullData();
                    if (CollectionUtils.isEmpty(rs)) {
                        return List.of(objectFullData);
                    }
                    return Streams.map(rs, r -> {
                        @SuppressWarnings("unchecked")
                        ObjectFullData<V> copy = (ObjectFullData<V>) ObjectFullDataMapper.INSTANCE.copy((ObjectFullData<AbstractValue>) objectFullData);
                        this.fullDataFillRelation(copy, r);
                        return copy;
                    }).toList();
                }).flatMap(Collection::stream).toList())
                .toList();
    }

    /**
     * object 转换为 ObjectFullData<V> 时做一些特殊处理
     *
     * @param fullData  fullData
     * @param objectMap {@literal <K, ObjectBodyEntity>}
     */
    public void handlerAfterObjectBodyConvertToFullData(ObjectFullData<V> fullData, Map<K, B> objectMap) {

    }

    public void fullDataFillRelation(ObjectFullData<V> fullData, R r) {
        fullData.setParentObjectId(r.getParentObjectId());
        fullData.setRelationType(r.getType());
        fullData.setBelongId(r.getBelongId());
    }

    public ObjectFullData<V> convert2Object(V objectValue) {
        ObjectFullData<V> objectFullData = new ObjectFullData<>();
        objectFullData.setObjectId(objectValue.getObjectId());
        objectFullData.setType(getObjectType(objectValue).getType());
        objectFullData.setValue(objectValue);
        return objectFullData;
    }

    /**
     * 数据保存处理
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveObjectData(List<O> objectList, List<B> objectBodyList, List<R> relationList) {
        if (!CollectionUtils.isEmpty(objectList)) {
            this.saveObjects(objectList);
        }
        if (!CollectionUtils.isEmpty(objectBodyList)) {
            this.saveObjectBodies(objectBodyList);
        }
        if (!CollectionUtils.isEmpty(relationList)) {
            this.saveRelations(relationList);
        }
    }

    public List<O> data2ObjectEntities(Collection<ObjectFullData<V>> objectData) {
        return objectData.stream().filter(k -> StatusEnum.updateObject(k.getStatus()))
                .map(this::data2ObjectEntity).toList();
    }

    public O data2ObjectEntity(ObjectFullData<V> data) {
        O entity = this.newObjectEntity();
        BizExceptionEnum.OBJECT_NEW_OBJECT_ENTITY_NONNULL.nonNull(entity, data.getValue().getObjectId());
        entity.setObjectId(data.getObjectId());
        entity.setType(data.getType());
        entity.setSpaceId(TraceContext.getSpaceIdOrDefault());
        entity.setDeleted(Boolean.FALSE);
        entity.setCreateUser(TraceContext.getUserIdOrDefault());
        return entity;
    }

    public List<B> data2ObjectBodyEntities(Collection<ObjectFullData<V>> objectData) {
        return objectData.stream().filter(k -> StatusEnum.updateObjectBody(k.getStatus()))
                .map(this::data2ObjectBodyEntity).toList();
    }

    public B data2ObjectBodyEntity(ObjectFullData<V> data) {
        B entity = this.newObjectBodyEntity();
        BizExceptionEnum.OBJECT_NEW_OBJECT_BODY_ENTITY_NONNULL.nonNull(entity, data.getValue().getObjectId());
        entity.setObjectId(data.getObjectId());
        entity.setName(data.getName());
        entity.setValue(Jsons.str(data.getValue()));
        entity.setCreateUser(TraceContext.getUserIdOrDefault());
        entity.setUpdateUser(TraceContext.getUserIdOrDefault());
        return entity;
    }

    public List<R> data2RelationEntities(Collection<ObjectFullData<V>> objectData) {
        return objectData.stream()
                .filter(k -> StatusEnum.updateRelation(k.getStatus()))
                .filter(k -> Objects.nonNull(k.getParentObjectId()))
                .map(this::data2RelationEntity).toList();
    }

    public R data2RelationEntity(ObjectFullData<V> data) {
        R entity = this.newRelationEntity();
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
    static class ObjectTemp<O extends ObjectEntityIdentity, R extends RelationEntityIdentity> {
        private String objectId;
        private O object;
        private List<R> relations;
    }

    public ObjectNode<String, ObjectFullData<AbstractValue>> find(Collection<String> objectIds) {
        Collection<ObjectFullData<AbstractValue>> fullData = Assign.build(objectIds)
                // objectId 转为 ObjectTemp
                .cast(e -> ObjectTemp.<O, R>builder().objectId(e).build())
                .parallel().interruptStrategy(InterruptStrategyEnum.ANY)
                // 查询 object
                .addAcquire(this::findObjects, O::getObjectId)
                .throwException()
                .addAction(ObjectTemp::getObjectId)
                .addAssemble(ObjectTemp::setObject)
                .backAcquire().backAssign()
                // 查询 relation 并按 belongId 分组
                .addAcquire(this.belongIdRelationsGroupMapping())
                .addAction(ObjectTemp::getObjectId)
                .addAssemble(ObjectTemp::setRelations)
                .backAcquire().backAssign().invoke()
                // ObjectTemp 转为 ObjectFullData<AbstractValue>
                .casts(es -> Streams.map(es, k -> {
                    if (Objects.isNull(k.getObject())) {
                        return List.<ObjectFullData<AbstractValue>>of();
                    }
                    List<String> ids = Streams.map(k.getRelations(), R::getObjectId).collect(Collectors.toList());
                    ids.add(k.getObjectId());
                    ObjectFullData<AbstractValue> data = new ObjectFullData<>();
                    O object = k.getObject();
                    data.setSpaceId(object.getSpaceId());
                    data.setType(object.getType());
                    data.setObjectId(k.getObjectId());
                    List<ObjectFullData<AbstractValue>> collect = Streams.map(k.getRelations(), r -> {
                        ObjectFullData<AbstractValue> copy = ObjectFullDataMapper.INSTANCE.copy(data);
                        copy.setObjectId(r.getObjectId());
                        copy.setParentObjectId(r.getParentObjectId());
                        copy.setBelongId(r.getBelongId());
                        copy.setRelationType(r.getType());
                        return copy;
                    }).collect(Collectors.toList());
                    collect.add(data);
                    return collect;
                }).flatMap(Collection::stream).toList())
                .parallel().interruptStrategy(InterruptStrategyEnum.ANY)
                // 查询 objectBody
                .addBranches(ObjectFullData::getType, this.assignerMap()).invoke()
                .toList();
        // 组成 tree 结构
        return ObjectNode.<String, ObjectFullData<AbstractValue>>buildTree().add(fullData);
    }

    public Function<Collection<String>, Map<String, List<R>>> belongIdRelationsGroupMapping() {
        return ks -> {
            List<R> relations = this.findRelationsByParentObjectIds(ks);
            List<R> relationsByBelongIds = this.findRelationsByBelongIds(ks);
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

    public Map<Integer, Function<Collection<ObjectFullData<AbstractValue>>, Assign<ObjectFullData<AbstractValue>>>> assignerMap() {
        Map<Integer, AbstractObjectProcessor<ObjectEntityIdentity, RelationEntityIdentity,
                ObjectBodyEntityIdentity, AbstractValue, ObjectTypeIdentity, Object>> processorMap = this.objectType2ProcessorMap();
        Map<Integer, Function<Collection<ObjectFullData<AbstractValue>>, Assign<ObjectFullData<AbstractValue>>>> assignerMap = HashMap.newHashMap(processorMap.size());
        processorMap.forEach((k, p) ->
                assignerMap.put(k, es -> this.assignByType(es, p)));
        return assignerMap;
    }

    public Assign<ObjectFullData<AbstractValue>> assignByType(Collection<ObjectFullData<AbstractValue>> es,
                                                              AbstractObjectProcessor<?, ?, ?, ?, ?, ?> processor) {
        return Assign.build(es)
                .addAcquire(processor::findObjectBodies, ObjectBodyEntityIdentity::getObjectId)
                .throwException()
                .addAction(ObjectFullData::getObjectId)
                .addAssemble((e, t) -> {
                    e.setName(t.getName());
                    e.setValue(processor.convert2Value(processor.getObjectType(e.getType()), t));
                }).backAcquire().backAssign();
    }

}