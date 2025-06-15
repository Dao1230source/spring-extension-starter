package org.source.spring.object.processor;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
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
import org.source.utility.tree.Tree;
import org.source.utility.tree.identity.AbstractNode;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Streams;
import org.springframework.aop.framework.AopContext;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Slf4j
public abstract class AbstractObjectProcessor<O extends ObjectEntityIdentity, R extends RelationEntityIdentity,
        B extends ObjectBodyEntityIdentity, V extends AbstractValue> {

    protected Tree<String, ObjectFullData<V>, ObjectNode<String, ObjectFullData<V>>> objectTree = ObjectNode.buildTree();

    /**
     * object
     */
    public abstract O newObjectEntity();

    public abstract List<O> findObjects(Collection<String> objectIds);

    public abstract void saveObjects(Collection<O> objects);

    /**
     * relation
     */
    public abstract R newRelationEntity();

    public abstract List<R> findRelationsByObjectIds(Collection<String> objectIds);

    public abstract List<R> findRelationsByParentObjectIds(Collection<String> parentObjectIds);

    public abstract List<R> findRelationsByBelongIds(Collection<String> belongIds);

    public abstract void saveRelations(Collection<R> relations);

    /**
     * object body
     */
    public abstract B newObjectBodyEntity();

    public abstract List<B> findObjectBodies(Collection<String> objectIds);

    public abstract void saveObjectBodies(Collection<B> objectBodies);

    /**
     * obtain type for object value
     */
    public abstract ObjectTypeIdentity getObjectType(V v);

    /**
     * convert entity to value
     */
    public abstract V toObjectValue(Integer type, ObjectBodyEntityIdentity objectBodyEntity);

    public abstract Map<Integer, ? extends AbstractObjectProcessor<? extends ObjectEntityIdentity, ? extends RelationEntityIdentity,
            ? extends ObjectBodyEntityIdentity, ? extends AbstractValue>> allObjectProcessors();

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
        ((AbstractObjectProcessor<O, R, B, V>) AopContext.currentProxy()).saveObjectData(objectList, objectBodyList, relationList);
        this.afterPersist();
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
            if (!StatusEnum.DATABASE.equals(n.getStatus()) && !n.getElement().equals(old.getElement())) {
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

    public boolean shouldSetParentObjectId() {
        return true;
    }

    /**
     * ObjectFullData 的key批量查询
     *
     * @param vs vs
     * @return {@literal Collection<ObjectFullData>}
     */
    @NonNull
    public Collection<ObjectFullData<V>> findFromDbAndConvert2FullData(Collection<V> vs) {
        Collection<B> objectBodies = this.findObjectBodyWhenWrite(vs);
        Map<String, ObjectTypeIdentity> valueTypeMap = Streams.toMap(vs, V::getObjectId, this::getObjectType);
        Collection<ObjectFullData<V>> objectFullData = this.convert2FullData(objectBodies, valueTypeMap);
        if (this.shouldSetParentObjectId()) {
            Collection<R> relations = this.findRelationWhenWrite(objectBodies);
            Map<String, List<String>> objectParentsMap = this.objectParentsMap(relations);
            objectFullData = this.fullDataSetParentObjectId(objectFullData, objectParentsMap);
        }
        return objectFullData;
    }

    /**
     * 写数据时，查询对象主体数据，有些自定义的对象可能不是通过objectId查询
     * 与之相对的{@link AbstractObjectProcessor }是读数据时的标准的通过objectId查询数据库
     *
     * @param vs vs
     * @return objectBodies
     */
    @NonNull
    public Collection<B> findObjectBodyWhenWrite(Collection<V> vs) {
        List<String> objectIds = Streams.map(vs, V::getObjectId).toList();
        return this.findObjectBodies(objectIds);
    }

    /**
     * 写数据时，查询关系数据
     *
     * @param bs vs
     * @return relations
     */
    @NonNull
    public Collection<R> findRelationWhenWrite(Collection<B> bs) {
        List<String> objectIds = Streams.map(bs, B::getObjectId).toList();
        return this.findRelationsByObjectIds(objectIds);
    }

    /**
     * 对应的父对象
     *
     * @param relations relations
     * @return {@literal <objectIds,parentObjectIds>}
     */
    public Map<String, List<String>> objectParentsMap(Collection<R> relations) {
        return Streams.of(relations).collect(Collectors.groupingBy(R::getObjectId,
                Collectors.mapping(R::getParentObjectId, Collectors.toList())));
    }

    public Collection<ObjectFullData<V>> convert2FullData(Collection<B> objectBodyEntities, Map<String, ObjectTypeIdentity> valueTypeMap) {
        return Streams.map(objectBodyEntities, k -> {
            ObjectTypeIdentity type = valueTypeMap.get(k.getObjectId());
            if (Objects.isNull(type)) {
                return null;
            }
            return type.<V>parse(k);
        }).filter(Objects::nonNull).toList();
    }

    public Collection<ObjectFullData<V>> fullDataSetParentObjectId(Collection<ObjectFullData<V>> fullData,
                                                                   Map<String, List<String>> objectParentsMap) {
        return fullData.stream().map(b -> {
            List<String> parentObjectIds = objectParentsMap.get(b.getObjectId());
            if (CollectionUtils.isEmpty(parentObjectIds)) {
                return List.of(b);
            } else if (parentObjectIds.size() == 1) {
                b.setParentObjectId(parentObjectIds.get(0));
                return List.of(b);
            } else {
                return Streams.map(parentObjectIds, r -> {
                    @SuppressWarnings("unchecked")
                    ObjectFullData<V> copy = (ObjectFullData<V>) ObjectFullDataMapper.INSTANCE.copy((ObjectFullData<AbstractValue>) b);
                    copy.setParentObjectId(r);
                    return copy;
                }).toList();
            }
        }).flatMap(Collection::stream).toList();
    }

    public @Nullable V valueFromObject(ObjectFullData<V> objectFullData) {
        return objectFullData.getValue();
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
        if (Objects.isNull(entity)) {
            return null;
        }
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
        if (Objects.isNull(entity)) {
            return null;
        }
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
        if (Objects.isNull(entity)) {
            return null;
        }
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
        Function<Collection<String>, Map<String, List<R>>> belongIdMap = ks -> {
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
        List<ObjectTemp<O, R>> list = Streams.map(new HashSet<>(objectIds), k -> ObjectTemp.<O, R>builder().objectId(k).build()).toList();
        List<ObjectTemp<O, R>> tempList = Assign.build(list)
                .parallel()
                .addAcquire(this::findObjects, O::getObjectId)
                .addAction(ObjectTemp::getObjectId)
                .addAssemble(ObjectTemp::setObject)
                .backAcquire().backAssign()
                .addAcquire(belongIdMap)
                .addAction(ObjectTemp::getObjectId)
                .addAssemble(ObjectTemp::setRelations)
                .backAcquire().backAssign().invoke().getMainData2List();
        List<ObjectFullData<AbstractValue>> objectFullData = Streams.map(tempList, k -> {
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
        }).flatMap(Collection::stream).toList();
        Map<Integer, ? extends AbstractObjectProcessor<? extends ObjectEntityIdentity, ? extends RelationEntityIdentity,
                ? extends ObjectBodyEntityIdentity, ? extends AbstractValue>> processorMap = this.allObjectProcessors();
        Map<Integer, Function<Collection<ObjectFullData<AbstractValue>>, Assign<ObjectFullData<AbstractValue>>>> assignerMap = HashMap.newHashMap(processorMap.size());
        processorMap.forEach((k, p) ->
                assignerMap.put(k, es -> this.assignByType(es, p)));
        List<ObjectFullData<AbstractValue>> fullData = Assign.build(objectFullData).addBranches(ObjectFullData::getType, assignerMap).invoke().getMainData2List();
        return ObjectNode.<String, ObjectFullData<AbstractValue>>buildTree().add(fullData);
    }

    public Assign<ObjectFullData<AbstractValue>> assignByType(Collection<ObjectFullData<AbstractValue>> es,
                                                              AbstractObjectProcessor<?, ?, ?, ?> processor) {
        return Assign.build(es)
                .addAcquire(processor::findObjectBodies, ObjectBodyEntityIdentity::getObjectId)
                .addAction(ObjectFullData::getObjectId)
                .addAssemble((e, t) -> {
                    e.setName(t.getName());
                    e.setValue(processor.toObjectValue(e.getType(), t));
                }).backAcquire().backAssign();
    }

}