package org.source.spring.object.processor;

import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.source.spring.exception.BizExceptionEnum;
import org.source.spring.object.AbstractValue;
import org.source.spring.object.StatusEnum;
import org.source.spring.object.data.ObjectFullData;
import org.source.spring.object.entity.ObjectBodyEntityDefiner;
import org.source.spring.object.entity.ObjectEntityDefiner;
import org.source.spring.object.entity.RelationEntityDefiner;
import org.source.spring.object.enums.ObjectTypeDefiner;
import org.source.spring.object.mapper.ObjectFullDataMapper;
import org.source.spring.object.tree.ObjectNode;
import org.source.spring.trace.TraceContext;
import org.source.spring.uid.Ids;
import org.source.spring.utility.SpringUtil;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
@Slf4j
public abstract class AbstractObjectProcessor<O extends ObjectEntityDefiner, R extends RelationEntityDefiner,
        B extends ObjectBodyEntityDefiner, V extends AbstractValue, T extends ObjectTypeDefiner, K> {
    protected static final Map<Integer, AbstractObjectProcessor<ObjectEntityDefiner, RelationEntityDefiner,
            ObjectBodyEntityDefiner, AbstractValue, ObjectTypeDefiner, Object>> ALL_TYPE_PROCESSORS = new ConcurrentHashMap<>();
    protected static final Map<Integer, Function<Collection<ObjectFullData<AbstractValue>>, Assign<ObjectFullData<AbstractValue>>>> ALL_TYPE_ASSIGNERS = new ConcurrentHashMap<>();

    protected final TransmittableThreadLocal<Tree<String, ObjectFullData<V>, ObjectNode<String, ObjectFullData<V>>>> objectTreeThreadLocal
            = TransmittableThreadLocal.withInitial(ObjectNode::buildTree);

    private final ObjectDbProcessorDefiner<O> objectDbProcessorDefiner;
    private final ObjectBodyDbProcessorDefiner<B, V, K> objectBodyDbProcessorDefiner;
    private final RelationDbProcessorDefiner<R> relationDbProcessorDefiner;

    public abstract Map<Integer, AbstractObjectProcessor<ObjectEntityDefiner, RelationEntityDefiner,
            ObjectBodyEntityDefiner, AbstractValue, ObjectTypeDefiner, Object>> objectType2ProcessorMap();

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
    public ObjectTypeDefiner getObjectType(V v) {
        ObjectTypeDefiner type = this.class2ObjectTypeMap().get(v.getClass());
        if (Objects.isNull(type)) {
            throw BizExceptionEnum.OBJECT_VALUE_CLASS_NOT_DEFINED.except("class:{}", v.getClass());
        }
        return type;
    }

    /**
     * convert entity to value
     */
    public ObjectTypeDefiner getObjectType(Integer type) {
        ObjectTypeDefiner objectType = this.type2ObjectTypeMap().get(type);
        if (Objects.isNull(objectType)) {
            throw BizExceptionEnum.OBJECT_TYPE_NOT_DEFINED.except("type:{}", type);
        }
        return objectType;
    }

    public V convert2Value(ObjectTypeDefiner objectType, ObjectBodyEntityDefiner objectBodyEntity) {
        return Jsons.obj(objectBodyEntity.getValue(), objectType.getValueClass());
    }

    public ObjectFullData<V> convert2FullData(ObjectTypeDefiner objectType, B entity) {
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
    public void merge(Collection<V> vs) {
        if (log.isDebugEnabled()) {
            log.debug("source values:{}", Jsons.str(vs));
        }
        Collection<V> maybeExistsDb = this.maybeExistsDb(vs);
        if (!CollectionUtils.isEmpty(maybeExistsDb)) {
            if (log.isDebugEnabled()) {
                log.debug("maybeExistsDb:{}", maybeExistsDb);
            }
            // 从数据中查询数据并添加到tree中
            List<ObjectFullData<V>> dataFromDbList = this.findFromDbAndConvert2FullData(maybeExistsDb);
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
    public void save() {
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
    public static class FindEntityAndToFullDataTemp<O, B, R, V extends AbstractValue> {
        private V value;
        private B objectBodyEntity;
        private ObjectFullData<V> fullData;
        private List<R> relations;

        private B parentObjectBodyEntity;
        private O parentObjectEntity;
        private ObjectFullData<V> parentFullData;
    }

    /**
     * ObjectFullData 的key批量查询
     *
     * @param vs vs
     * @return {@literal Collection<ObjectFullData >}
     */
    public List<ObjectFullData<V>> findFromDbAndConvert2FullData(Collection<V> vs) {
        Map<String, V> valueTypeMap = Streams.toMap(vs, this.getValueIdGetter());
        return Assign.build(vs)
                .cast(v -> FindEntityAndToFullDataTemp.<O, B, R, V>builder().value(v).build())
                .name("get objectBody and parent objectBody")
                // 查询 objectBody
                .addAcquire(this.objectBodyDbProcessorDefiner::findObjectBodiesByKeys, this.objectBodyDbProcessorDefiner::objectBodyToKey)
                .throwException()
                .afterProcessor((e, map) ->
                        this.handlerAfterObjectBodyConvertToFullData(e.getFullData(), map))
                .addAction(k -> this.objectBodyDbProcessorDefiner.valueToKey(k.getValue()))
                // 给 objectBodyEntity、ObjectFullData<V > 赋值
                .addAssemble((e, b) -> {
                    e.setObjectBodyEntity(b);
                    String objectBodyId = this.getObjectBodyIdGetter().apply(b);
                    V v = valueTypeMap.get(objectBodyId);
                    BizExceptionEnum.OBJECT_CANNOT_FIND_VALUE.nonNull(v, objectBodyId);
                    ObjectTypeDefiner type = this.getObjectType(v);
                    e.setFullData(this.convert2FullData(type, b));
                })
                .backAcquire()
                .addAction(k -> this.objectBodyDbProcessorDefiner.valueToParentKey(k.getValue()))
                .addAssemble(FindEntityAndToFullDataTemp::setParentObjectBodyEntity)
                .backAcquire().backAssign()
                .addBranch(e -> Objects.nonNull(e.getParentObjectBodyEntity()))
                .name("get parent fullData")
                .addAcquire(this.objectDbProcessorDefiner::findObjects, O::getObjectId)
                .addAction(e -> e.getParentObjectBodyEntity().getObjectId())
                .addAssemble((e, o) -> {
                    e.setParentObjectEntity(o);
                    ObjectTypeDefiner type = this.getObjectType(o.getType());
                    e.setParentFullData(this.convert2FullData(type, e.getParentObjectBodyEntity()));
                })
                .backAcquire().backAssign()
                .backSuper()
                // 这里新建分支是因为需要先查询 objectBody，有依赖关系
                .addBranch(e -> Objects.nonNull(e.getObjectBodyEntity()))
                .name("get relations")
                // 根据 objectId 查询 relations ，按objectId 分组，因为一个对象可能有多个父级
                .<String, List<R>>addAcquire(ks -> Streams.of(this.relationDbProcessorDefiner.findRelationsByObjectIds(ks)).collect(Collectors.groupingBy(R::getObjectId)))
                .addAction(e -> e.getObjectBodyEntity().getObjectId())
                .addAssemble(FindEntityAndToFullDataTemp::setRelations)
                .backAcquire().backAssign()
                .backSuper()
                // 最终执行
                .invoke()
                // 展开，转换为 ObjectFullData<V >
                .casts(es -> Streams.map(es, e -> {
                    List<ObjectFullData<V>> fullData = new ArrayList<>(this.processFullData(e.getFullData(), e.getRelations()));
                    if (Objects.nonNull(e.getParentFullData())) {
                        fullData.add(e.getParentFullData());
                    }
                    return fullData;
                }).flatMap(Collection::stream).toList())
                .toList();
    }

    /**
     * object 转换为 ObjectFullData<V > 时做一些特殊处理
     *
     * @param fullData  fullData
     * @param objectMap {@literal <K, ObjectBodyEntity >}
     */
    public void handlerAfterObjectBodyConvertToFullData(ObjectFullData<V> fullData, Map<K, B> objectMap) {

    }

    public List<ObjectFullData<V>> processFullData(ObjectFullData<V> objectFullData, List<R> rs) {
        if (Objects.isNull(objectFullData)) {
            return List.of();
        }
        if (CollectionUtils.isEmpty(rs)) {
            return List.of(objectFullData);
        } else {
            return Streams.map(rs, r -> {
                @SuppressWarnings("unchecked")
                ObjectFullData<V> copy = (ObjectFullData<V>) ObjectFullDataMapper.INSTANCE.copy((ObjectFullData<AbstractValue>) objectFullData);
                this.fullDataFillRelation(copy, r);
                return copy;
            }).toList();
        }
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
            this.objectDbProcessorDefiner.saveObjects(objectList);
        }
        if (!CollectionUtils.isEmpty(objectBodyList)) {
            this.objectBodyDbProcessorDefiner.saveObjectBodies(objectBodyList);
        }
        if (!CollectionUtils.isEmpty(relationList)) {
            this.relationDbProcessorDefiner.saveRelations(relationList);
        }
    }

    public List<O> data2ObjectEntities(Collection<ObjectFullData<V>> objectData) {
        return objectData.stream().filter(k -> StatusEnum.updateObject(k.getStatus()))
                .map(this::data2ObjectEntity).toList();
    }

    public O data2ObjectEntity(ObjectFullData<V> data) {
        O entity = this.objectDbProcessorDefiner.newObjectEntity();
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
        B entity = this.objectBodyDbProcessorDefiner.newObjectBodyEntity();
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
        R entity = this.relationDbProcessorDefiner.newRelationEntity();
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

    public ObjectNode<String, ObjectFullData<AbstractValue>> find(Collection<String> objectIds) {
        Collection<ObjectFullData<AbstractValue>> fullData = Assign.build(objectIds)
                // objectId 转为 ObjectTemp
                .cast(e -> ObjectTemp.<O, R>builder().objectId(e).build())
                .parallel().interruptStrategy(InterruptStrategyEnum.ANY)
                // 查询 object
                .addAcquire(this.objectDbProcessorDefiner::findObjects, O::getObjectId)
                .throwException()
                .addAction(ObjectTemp::getObjectId)
                .addAssemble(ObjectTemp::setObject)
                .backAcquire().backAssign()
                // 查询 relation 并按 belongId 分组
                .addAcquire(this.belongIdRelationsGroupMapping())
                .addAction(ObjectTemp::getObjectId)
                .addAssemble(ObjectTemp::setRelations)
                .backAcquire().backAssign().invoke()
                // ObjectTemp 转为 ObjectFullData<AbstractValue >
                .casts(es -> Streams.map(es, k -> {
                    if (Objects.isNull(k.getObject())) {
                        return List.<ObjectFullData<AbstractValue>>of();
                    }
                    ObjectFullData<AbstractValue> data = new ObjectFullData<>();
                    O object = k.getObject();
                    data.setSpaceId(object.getSpaceId());
                    data.setType(object.getType());
                    data.setObjectId(k.getObjectId());
                    if (Objects.isNull(k.getRelations())) {
                        return List.of(data);
                    }
                    return Streams.map(k.getRelations(), r -> {
                        ObjectFullData<AbstractValue> copy = ObjectFullDataMapper.INSTANCE.copy(data);
                        copy.setObjectId(r.getObjectId());
                        copy.setParentObjectId(r.getParentObjectId());
                        copy.setBelongId(r.getBelongId());
                        copy.setRelationType(r.getType());
                        return copy;
                    }).collect(Collectors.toList());
                }).flatMap(Collection::stream).toList())
                .parallel().interruptStrategy(InterruptStrategyEnum.ANY)
                // 查询 objectBody
                .addBranches(ObjectFullData::getType, this.allTypeAssigners()).invoke()
                .toList();
        // 组成 tree 结构
        return ObjectNode.<String, ObjectFullData<AbstractValue>>buildTree().add(fullData);
    }

    public Function<Collection<String>, Map<String, List<R>>> belongIdRelationsGroupMapping() {
        return ks -> {
            List<R> relations = this.relationDbProcessorDefiner.findRelationsByParentObjectIds(ks);
            List<R> relationsByBelongIds = this.relationDbProcessorDefiner.findRelationsByBelongIds(ks);
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Map<Integer, AbstractObjectProcessor<ObjectEntityDefiner, RelationEntityDefiner,
            ObjectBodyEntityDefiner, AbstractValue, ObjectTypeDefiner, Object>> allTypeProcessors() {
        if (ALL_TYPE_PROCESSORS.isEmpty()) {
            Map<String, AbstractObjectProcessor> beansOfType = SpringUtil.getBeansOfType(AbstractObjectProcessor.class);
            beansOfType.forEach((k, v) -> ALL_TYPE_PROCESSORS.putAll(v.objectType2ProcessorMap()));
        }
        return ALL_TYPE_PROCESSORS;
    }

    public Map<Integer, Function<Collection<ObjectFullData<AbstractValue>>, Assign<ObjectFullData<AbstractValue>>>> allTypeAssigners() {
        if (ALL_TYPE_ASSIGNERS.isEmpty()) {
            this.allTypeProcessors().forEach((k, p) ->
                    ALL_TYPE_ASSIGNERS.put(k, es -> assignByType(es, p)));
        }
        return ALL_TYPE_ASSIGNERS;
    }

    public Assign<ObjectFullData<AbstractValue>> assignByType(Collection<ObjectFullData<AbstractValue>> es,
                                                              AbstractObjectProcessor<?, ?, ?, ?, ?, ?> processor) {
        return Assign.build(es)
                .addAcquire(processor.objectBodyDbProcessorDefiner::findObjectBodies, ObjectBodyEntityDefiner::getObjectId)
                .throwException()
                .addAction(ObjectFullData::getObjectId)
                .addAssemble((e, t) -> {
                    e.setName(t.getName());
                    e.setValue(processor.convert2Value(processor.getObjectType(e.getType()), t));
                }).backAcquire().backAssign();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Collection<String> objectIds) {
        this.objectDbProcessorDefiner.deleteObjects(objectIds);
    }

    @Transactional(rollbackFor = Exception.class)
    public void remove(Collection<String> objectIds) {
        this.objectDbProcessorDefiner.removeObjects(objectIds);
        this.objectBodyDbProcessorDefiner.removeObjectBodies(objectIds);
        this.relationDbProcessorDefiner.removeRelations(objectIds);
    }
}