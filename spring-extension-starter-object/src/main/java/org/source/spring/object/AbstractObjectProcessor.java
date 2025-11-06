package org.source.spring.object;

import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.source.spring.common.exception.SpExtExceptionEnum;
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
import org.source.spring.uid.Uids;
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
import java.util.stream.Stream;

@AllArgsConstructor
@Getter
@Slf4j
public abstract class AbstractObjectProcessor<O extends ObjectEntityDefiner, R extends RelationEntityDefiner,
        B extends ObjectBodyEntityDefiner, V extends AbstractValue, T extends ObjectTypeDefiner, K> {

    protected final TransmittableThreadLocal<EnhanceTree<String, ObjectElement<V>, ObjectNode<V>>> objectTreeThreadLocal
            = TransmittableThreadLocal.withInitial(() -> EnhanceTree.of(new ObjectNode<>()));

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

    public ObjectNode<AbstractValue> findByObjectIds(Collection<String> objectIds) {
        return this.find(objectIds);
    }

    public EnhanceTree<String, ObjectElement<V>, ObjectNode<V>> getObjectTree() {
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
        try {
            if (log.isDebugEnabled()) {
                log.debug("source values:{}", Jsons.str(vs));
            }
            Collection<V> maybeExistsInDb = this.needValidExistsInDb(vs);
            if (!CollectionUtils.isEmpty(maybeExistsInDb)) {
                if (log.isDebugEnabled()) {
                    log.debug("maybeExistsInDb:{}", maybeExistsInDb);
                }
                // 从数据中查询数据并添加到tree中
                List<ObjectElement<V>> dataFromDbList = this.findFromDbAndConvertToObjectElement(maybeExistsInDb);
                if (log.isDebugEnabled()) {
                    log.debug("dataFromDbList:{}", dataFromDbList);
                }
                this.handleDbDataTree().add(dataFromDbList);
            }
            Collection<ObjectElement<V>> objectElements = Streams.map(vs, this::convert2ObjectElement).filter(Objects::nonNull).toList();
            this.handleValueDataTree().add(objectElements);
            this.afterTransfer();
        } catch (Exception e) {
            this.getObjectTree().clear();
            log.error("AbstractObjectProcessor.merge error", e);
            throw SpExtExceptionEnum.OBJECT_MERGE_ERROR.except(e);
        }
    }

    /**
     * 需要校验是否在数据库是否存在的V
     *
     * @param vs vs
     * @return vs
     */
    public Collection<V> needValidExistsInDb(Collection<V> vs) {
        Map<String, ObjectNode<V>> idMap = this.getObjectTree().getIdMap();
        return Streams.retain(vs, v -> {
            ObjectNode<V> node = idMap.get(this.getValueIdGetter().apply(v));
            return Objects.isNull(node) || !StatusEnum.DATABASE.equals(node.getStatus());
        }).toList();
    }

    public EnhanceTree<String, ObjectElement<V>, ObjectNode<V>> handleDbDataTree() {
        EnhanceTree<String, ObjectElement<V>, ObjectNode<V>> tree = this.getObjectTree();
        tree.setIdGetter(n -> Node.getProperty(n, this.getElementIdGetter()));
        tree.setParentIdGetter(n -> Node.getProperty(n, this.getElementParentIdGetter()));
        tree.setAfterCreateHandler(n -> n.setStatus(StatusEnum.DATABASE));
        tree.setMergeHandler(this::mergeNode);
        return tree;
    }

    public EnhanceTree<String, ObjectElement<V>, ObjectNode<V>> handleValueDataTree() {
        EnhanceTree<String, ObjectElement<V>, ObjectNode<V>> tree = this.getObjectTree();
        tree.setIdGetter(n -> Node.getProperty(n, this.getElementIdGetter()));
        tree.setParentIdGetter(n -> Node.getProperty(n, this.getElementParentIdGetter()));
        tree.setAfterCreateHandler(n -> {
            n.setStatus(StatusEnum.CREATED);
            if (Objects.isNull(n.getElement().getObjectId())) {
                n.getElement().setObjectId(Uids.stringId());
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
        List<ObjectNode<V>> objectNodes = this.obtainObjectData();
        if (CollectionUtils.isEmpty(objectNodes)) {
            return;
        }
        ObjectNodesToEntitiesTemp<O, B, R> temp = this.nodeConvertToEntities(objectNodes);
        List<O> objectList = temp.getObjectList();
        List<B> objectBodyList = temp.getObjectBodyList();
        List<R> relationList = temp.getRelationList();
        ((AbstractObjectProcessor<O, R, B, V, T, K>) AopContext.currentProxy()).saveObjectData(objectList, objectBodyList, relationList);
        this.objectTreeThreadLocal.remove();
        this.afterPersist();
    }

    /**
     * 对 this.getDocTree() 做一些操作
     */
    public void beforePersist() {
    }

    public List<ObjectNode<V>> obtainObjectData() {
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

    public ObjectNodesToEntitiesTemp<O, B, R> nodeConvertToEntities(List<ObjectNode<V>> objectNodes) {
        List<O> objectList = new ArrayList<>(objectNodes.size());
        List<B> objectBodyList = new ArrayList<>(objectNodes.size());
        List<R> relationList = new ArrayList<>(objectNodes.size());
        objectNodes.stream().filter(n -> Objects.nonNull(n.getElement())).forEach(n -> {
            if (log.isDebugEnabled()) {
                log.debug("nodeConvertToEntities, objectNodes:{}", Jsons.str(n.getElement()));
            }
            if (StatusEnum.updateObject(n.getStatus())) {
                objectList.add(this.data2ObjectEntity(n));
            }
            if (StatusEnum.updateObjectBody(n.getStatus())) {
                objectBodyList.add(this.data2ObjectBodyEntity(n));
            }
            if (StatusEnum.updateRelation(n.getStatus()) && !CollectionUtils.isEmpty(n.getParents())) {
                Iterator<Integer> iterator = Objects.requireNonNullElse(n.getRelationTypes(), List.<Integer>of()).iterator();
                n.getParents().stream().filter(Node::hasElement).forEach(p ->
                        relationList.add(this.data2RelationEntity(n, p, iterator.hasNext() ? iterator.next() : null)));
            }
        });
        return new ObjectNodesToEntitiesTemp<>(objectList, objectBodyList, relationList);
    }

    public O data2ObjectEntity(ObjectNode<V> node) {
        O entity = this.objectDbHandler.newObjectEntity();
        ObjectElement<V> element = node.getElement();
        entity.setObjectId(element.getObjectId());
        entity.setType(element.getType());
        entity.setSpaceId(TraceContext.getSpaceIdOrDefault());
        entity.setDeleted(Boolean.FALSE);
        entity.setCreateUser(TraceContext.getUserIdOrDefault());
        return entity;
    }

    public B data2ObjectBodyEntity(ObjectNode<V> node) {
        B entity = this.objectBodyDbHandler.newObjectBodyEntity();
        ObjectElement<V> element = node.getElement();
        entity.setObjectId(element.getObjectId());
        entity.setName(element.getName());
        entity.setValue(Jsons.str(element.getValue()));
        entity.setCreateUser(TraceContext.getUserIdOrDefault());
        entity.setUpdateUser(TraceContext.getUserIdOrDefault());
        return entity;
    }

    public R data2RelationEntity(ObjectNode<V> node, ObjectNode<V> parent, Integer relationType) {
        R entity = this.relationDbHandler.newRelationEntity();
        ObjectElement<V> element = node.getElement();
        entity.setObjectId(element.getObjectId());
        entity.setParentObjectId(parent.getElement().getObjectId());
        entity.setType(Objects.requireNonNullElseGet(relationType, element::getRelationType));
        entity.setSorted(element.getSorted());
        entity.setCreateUser(TraceContext.getUserIdOrDefault());
        return entity;
    }


    public void afterPersist() {
        this.getObjectTree().forEach((i, n) -> n.setStatus(StatusEnum.DATABASE));
    }

    public ObjectNode<V> mergeNode(ObjectNode<V> n, ObjectNode<V> old) {
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
                ObjectElement<V> newElement = n.getElement();
                ObjectElement<V> oldElement = old.getElement();
                newElement.setObjectId(oldElement.getObjectId());
                old.setElement(this.mergeValue(newElement, oldElement));
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

    public boolean nodeEquals(ObjectNode<V> n, ObjectNode<V> old) {
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

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class FindEntityAndToObjectElementTemp<O, B, R, V extends AbstractValue> {
        private V value;

        private String objectId;
        private B objectBodyEntity;
        private O objectEntity;
        private ObjectElement<V> objectElement;

        private String parentObjectId;
        private B parentObjectBodyEntity;
        private O parentObjectEntity;
        private ObjectElement<V> parentObjectElement;

        private List<R> relations;
    }

    /**
     * ObjectElement 的key批量查询
     *
     * @param vs vs
     * @return {@literal Collection<ObjectElement<V>>}
     */
    public List<ObjectElement<V>> findFromDbAndConvertToObjectElement(Collection<V> vs) {
        Assign<FindEntityAndToObjectElementTemp<O, B, R, V>> assign = Assign.build(vs)
                .cast(v -> FindEntityAndToObjectElementTemp.<O, B, R, V>builder().value(v).build())
                .name("get_objectBodies")
                // 查询 objectBody
                .addAcquire(this.objectBodyDbHandler::findObjectBodiesByKeys, this.objectBodyDbHandler::objectBodyToKey)
                .throwException()
                // ObjectBody key
                .addAction(k -> this.objectBodyDbHandler.valueToKey(k.getValue()))
                .addAssemble((e, b) -> {
                    e.setObjectBodyEntity(b);
                    e.setObjectId(b.getObjectId());
                })
                .backAcquire()
                // ObjectBodyEntity parentKey
                .addAction(k -> this.objectBodyDbHandler.valueToParentKey(k.getValue()))
                .addAssemble((e, b) -> {
                    e.setParentObjectBodyEntity(b);
                    e.setParentObjectId(b.getObjectId());
                })
                .backAcquire().backAssign()
                .addBranch(e -> Objects.nonNull(e.getObjectBodyEntity()) || Objects.nonNull(e.getParentObjectBodyEntity()))
                .name("get_objects")
                .addAcquire(this.objectDbHandler::findObjects, O::getObjectId)
                .addAction(FindEntityAndToObjectElementTemp::getObjectId)
                .addAssemble((e, o) -> {
                    e.setObjectEntity(o);
                    ObjectTypeDefiner type = this.objectTypeHandler.getObjectType(o.getType());
                    e.setObjectElement(this.objectTypeHandler.convertToObjectElement(type, e.getObjectBodyEntity()));
                })
                .backAcquire()
                .addAction(FindEntityAndToObjectElementTemp::getParentObjectId)
                .addAssemble((e, o) -> {
                    e.setParentObjectEntity(o);
                    ObjectTypeDefiner type = this.objectTypeHandler.getObjectType(o.getType());
                    e.setParentObjectElement(this.objectTypeHandler.convertToObjectElement(type, e.getParentObjectBodyEntity()));
                })
                .backAcquire().backAssign().backSuper()
                // 这里新建分支是因为需要先查询 objectBody，有依赖关系
                .addBranch(e -> Objects.nonNull(e.getObjectElement()))
                .name("get_relations")
                // 根据 objectId 查询 relations ，按objectId 分组，因为一个对象可能有多个父级
                .<String, List<R>>addAcquire(ks -> Streams.of(this.relationDbHandler.findRelationsByObjectIds(ks)).collect(Collectors.groupingBy(R::getObjectId)))
                .addAction(e -> e.getObjectBodyEntity().getObjectId())
                .addAssemble(FindEntityAndToObjectElementTemp::setRelations)
                .backAcquire().backAssign()
                .backSuper()
                // 最终执行
                .invoke();
        // 展开，转换为 ObjectElement<V>
        List<FindEntityAndToObjectElementTemp<O, B, R, V>> tempList = assign.toList();
        List<ObjectElement<V>> objectElements = Streams.map(tempList, e -> {
            List<ObjectElement<V>> elements = new ArrayList<>(this.flatRelations(e.getObjectElement(), e.getRelations()));
            if (Objects.nonNull(e.getParentObjectElement())) {
                elements.add(e.getParentObjectElement());
            }
            return elements;
        }).flatMap(Collection::stream).toList();
        Set<String> objectIds = Streams.map(objectElements, k -> new String[]{k.getObjectId(), k.getParentObjectId()})
                .flatMap(Stream::of).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> otherParentObjectIds = Streams.map(tempList, k ->
                        Streams.map(k.getRelations(), R::getParentObjectId).toList())
                .flatMap(Collection::stream).filter(objectIds::contains).collect(Collectors.toSet());
        List<ObjectElement<V>> otherElements = Assign.build(otherParentObjectIds)
                .cast(k -> FindEntityAndToObjectElementTemp.<O, B, R, V>builder().objectId(k).build())
                .addAcquire(this.objectDbHandler::findObjects, O::getObjectId)
                .addAction(FindEntityAndToObjectElementTemp::getObjectId)
                .addAssemble(FindEntityAndToObjectElementTemp::setObjectEntity)
                .backAcquire().backAssign()
                .addAcquire(this.objectBodyDbHandler::findObjectBodies, B::getObjectId)
                .addAction(FindEntityAndToObjectElementTemp::getObjectId)
                .addAssemble((e, b) -> {
                    e.setObjectBodyEntity(b);
                    ObjectTypeDefiner type = this.objectTypeHandler.getObjectType(e.getObjectEntity().getType());
                    e.setObjectElement(this.objectTypeHandler.convertToObjectElement(type, e.getObjectBodyEntity()));
                })
                .backAcquire().backAssign().invoke()
                .toList().stream().map(FindEntityAndToObjectElementTemp::getObjectElement).toList();
        return Stream.concat(objectElements.stream(), otherElements.stream()).toList();
    }

    public List<ObjectElement<V>> flatRelations(ObjectElement<V> objectElement, List<R> rs) {
        if (Objects.isNull(objectElement)) {
            return List.of();
        }
        if (CollectionUtils.isEmpty(rs)) {
            return List.of(objectElement);
        }
        return Streams.map(rs, r -> {
            @SuppressWarnings("unchecked")
            ObjectElement<V> copy = (ObjectElement<V>) ObjectElementMapper.INSTANCE.copy((ObjectElement<AbstractValue>) objectElement);
            return this.flatRelation(copy, r);
        }).toList();
    }

    public ObjectElement<V> flatRelation(ObjectElement<V> objectElement, R r) {
        objectElement.setParentObjectId(r.getParentObjectId());
        objectElement.setRelationType(r.getType());
        return objectElement;
    }

    public ObjectElement<V> convert2ObjectElement(V objectValue) {
        ObjectElement<V> objectElement = new ObjectElement<>();
        objectElement.setObjectId(objectValue.getObjectId());
        objectElement.setType(this.objectTypeHandler.getObjectType(objectValue).getType());
        objectElement.setName(objectValue.getName());
        objectElement.setRelationType(objectValue.getRelationType());
        objectElement.setValue(objectValue);
        objectElement.setSorted(objectValue.getSorted());
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
        private O object;
        private List<R> relations;
    }

    protected ObjectNode<AbstractValue> find(Collection<String> objectIds) {
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
                // ObjectTemp 转为 ObjectElement<AbstractValue>
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
                        copy.setRelationType(r.getType());
                        return copy;
                    }).collect(Collectors.toList());
                }).flatMap(Collection::stream).toList())
                .parallel().interruptStrategy(InterruptStrategyEnum.ANY)
                // 查询 objectBody
                .addBranches(ObjectElement::getType, this.objectTypeHandler.allTypeAssigners()).invoke()
                .toList();
        // 组成 tree 结构
        return EnhanceTree.of(new ObjectNode<>()).add(fullData);
    }

    protected Function<Collection<String>, Map<String, List<R>>> belongIdRelationsGroupMapping() {
        return ks -> {
            List<R> relations = this.relationDbHandler.findRelationsByParentObjectIds(ks);
            return Streams.groupBy(relations, R::getParentObjectId);
        };
    }

}