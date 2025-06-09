package org.source.spring.object.processor;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.source.spring.doc.enums.DocObjectTypeEnum;
import org.source.spring.object.AbstractValue;
import org.source.spring.object.StatusEnum;
import org.source.spring.object.data.ObjectFullData;
import org.source.spring.object.entity.ObjectBodyEntityIdentity;
import org.source.spring.object.entity.ObjectEntityIdentity;
import org.source.spring.object.entity.RelationEntityIdentity;
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

    protected Tree<String, V, ObjectNode<String, V>> docTree = ObjectNode.buildTree();

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

    public abstract Map<String, List<R>> findRelations(Collection<String> objectIds);

    public abstract void saveRelations(Collection<R> relations);

    /**
     * object body
     */
    public abstract B newObjectBodyEntity();

    public abstract List<B> findObjectBodies(Collection<String> referenceIds);

    public abstract void saveObjectBodies(Collection<B> objectBodies);

    /**
     * obtain type for object value
     */
    public abstract Integer getObjectType(V v);

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
        Collection<V> notExistsDb = this.maybeExistsDb(vs);
        if (!CollectionUtils.isEmpty(notExistsDb)) {
            // 从数据中查询数据并添加到tree中
            List<V> dataFromDbList = this.findFromDbAndConvert2Data(notExistsDb).stream().map(this::valueFromObject).toList();
            // 如果相同 key 的数据已存在，更新 objectId
            this.getDocTree().add(dataFromDbList,
                    true,
                    n -> n.setStatus(StatusEnum.DATABASE),
                    this::mergeNode,
                    null);
        }
        this.getDocTree().add(vs, true, n -> n.setStatus(StatusEnum.CREATED),
                this::mergeNode, null);
        this.afterTransfer();
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
        this.getDocTree().forEach((i, n) -> {
            if (StatusEnum.DATABASE.equals(n.getOldStatus())
                    && Objects.nonNull(n.getOldElement())
                    && n.getOldElement().equals(n.getElement())) {
                n.setStatus(StatusEnum.DATABASE);
            }
        });
    }

    public List<ObjectFullData<V>> obtainObjectData() {
        return this.getDocTree().getIdMap().values().stream()
                .filter(k -> !StatusEnum.DATABASE.equals(k.getStatus()))
                .map(AbstractNode::getElement).filter(Objects::nonNull).map(this::convert2Object).toList();
    }

    public void afterPersist() {
        this.getDocTree().forEach((i, n) -> n.setStatus(StatusEnum.DATABASE));
    }

    public ObjectNode<String, V> mergeNode(ObjectNode<String, V> n, ObjectNode<String, V> old) {
        log.debug("new object id:{}", n.getId());
        if (Objects.isNull(old)) {
            // 默认CREATED
            n.setStatus(Objects.requireNonNullElse(n.getStatus(), StatusEnum.CREATED));
            log.debug("old object not exists");
            return n;
        }
        log.debug("old object id:{}", old.getId());
        // 现有的node已保存到数据库
        if (StatusEnum.DATABASE.equals(old.getStatus())) {
            log.debug("old object from database");
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
            log.debug("new object from database");
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
    public V mergeValue(V n, V old) {
        return n;
    }

    public Collection<V> maybeExistsDb(Collection<V> vs) {
        List<V> vsFromDb = this.getDocTree().find(n -> StatusEnum.DATABASE.equals(n.getStatus()))
                .stream().map(AbstractNode::getElement).filter(Objects::nonNull).toList();
        return Streams.notRetain(vs, AbstractValue::getId, vsFromDb).toList();
    }

    /**
     * ObjectFullData 的key批量查询
     *
     * @param vs vs
     * @return {@literal Collection<ObjectFullData>}
     */
    @NonNull
    public Collection<ObjectFullData<V>> findFromDbAndConvert2Data(Collection<V> vs) {
        return this.convert2Data(this.findFromDb(vs));
    }

    @NonNull
    public Collection<B> findFromDb(Collection<V> vs) {
        return List.of();
    }

    public Collection<ObjectFullData<V>> convert2Data(Collection<B> objectBodyEntities) {
        return Streams.map(objectBodyEntities, k -> {
            DocObjectTypeEnum type = DocObjectTypeEnum.getByType(k.getType());
            if (Objects.isNull(type)) {
                return null;
            }
            return type.<V>parse(k);
        }).filter(Objects::nonNull).toList();
    }

    public @Nullable V valueFromObject(ObjectFullData<V> objectFullData) {
        return objectFullData.getValue();
    }

    public ObjectFullData<V> convert2Object(V objectValue) {
        ObjectFullData<V> objectFullData = new ObjectFullData<>();
        objectFullData.setObjectId(objectValue.getObjectId());
        objectFullData.setType(getObjectType(objectValue));
        objectFullData.setValue(objectValue);
        // 如果objectId为null，表明是新增数据
        if (Objects.isNull(objectValue.getObjectId())) {
            objectFullData.setObjectId(Ids.stringId());
            objectValue.setObjectId(objectFullData.getObjectId());
        }
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
        return objectData.stream().map(this::data2ObjectEntity).toList();
    }

    public O data2ObjectEntity(ObjectFullData<V> data) {
        O entity = this.newObjectEntity();
        if (Objects.isNull(entity)) {
            return null;
        }
        entity.setObjectId(data.getObjectId());
        entity.setSpaceId(TraceContext.getSpaceIdOrDefault());
        entity.setDeleted(Boolean.FALSE);
        entity.setCreateUser(TraceContext.getUserIdOrDefault());
        return entity;
    }

    public List<B> data2ObjectBodyEntities(Collection<ObjectFullData<V>> objectData) {
        return objectData.stream().map(this::data2ObjectBodyEntity).toList();
    }

    public B data2ObjectBodyEntity(ObjectFullData<V> data) {
        B entity = this.newObjectBodyEntity();
        if (Objects.isNull(entity)) {
            return null;
        }
        entity.setObjectId(data.getObjectId());
        entity.setKey(data.getKey());
        entity.setValue(Jsons.str(data.getValue()));
        entity.setType(data.getType());
        entity.setCreateUser(TraceContext.getUserIdOrDefault());
        entity.setUpdateUser(TraceContext.getUserIdOrDefault());
        return entity;
    }

    public List<R> data2RelationEntities(Collection<ObjectFullData<V>> objectData) {
        return objectData.stream().filter(k -> Objects.nonNull(k.getParentId())).map(this::data2RelationEntity).toList();
    }

    public R data2RelationEntity(ObjectFullData<V> objectFullData) {
        R entity = this.newRelationEntity();
        if (Objects.isNull(entity)) {
            return null;
        }
        entity.setObjectId(objectFullData.getObjectId());
        entity.setParentObjectId(objectFullData.getParentObjectId());
        entity.setType(objectFullData.getRelationType());
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

    public List<ObjectFullData<AbstractValue>> find(Collection<String> objectIds) {
        List<ObjectTemp<O, R>> list = Streams.map(new HashSet<>(objectIds), k -> ObjectTemp.<O, R>builder().objectId(k).build()).toList();
        List<ObjectTemp<O, R>> tempList = Assign.build(list)
                .parallel()
                .addAcquire(this::findObjects, O::getObjectId)
                .addAction(ObjectTemp::getObjectId)
                .addAssemble(ObjectTemp::setObject)
                .backAcquire().backAssign()
                .addAcquire(this::findRelations)
                .addAction(ObjectTemp::getObjectId)
                .addAssemble(ObjectTemp::setRelations)
                .backAcquire().backAssign().invoke().getMainData2List();
        List<ObjectFullData<AbstractValue>> objectFullData = Streams.map(tempList, k -> {
            List<String> ids = Streams.map(k.getRelations(), R::getObjectId).collect(Collectors.toList());
            ids.add(k.getObjectId());
            ObjectFullData<AbstractValue> data = new ObjectFullData<>();
            data.setObjectId(k.getObjectId());
            if (Objects.nonNull(k.getObject())) {
                O object = k.getObject();
                data.setSpaceId(object.getSpaceId());
            }
            List<ObjectFullData<AbstractValue>> collect = Streams.map(k.getRelations(), r -> {
                ObjectFullData<AbstractValue> copy = ObjectFullDataMapper.INSTANCE.copy(data);
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
                assignerMap.put(k, es ->
                        Assign.build(es)
                                .addAcquire(p::findObjectBodies, ObjectBodyEntityIdentity::getObjectId)
                                .addAction(ObjectFullData::getObjectId)
                                .addAssemble((e, t) -> {
                                    e.setKey(t.getKey());
                                    e.setValue(p.toObjectValue(e.getType(), t));
                                    e.setType(t.getType());
                                }).backAcquire().backAssign()));
        return Assign.build(objectFullData).addBranches(ObjectFullData::getType, assignerMap).invoke().getMainData2List();
    }

}