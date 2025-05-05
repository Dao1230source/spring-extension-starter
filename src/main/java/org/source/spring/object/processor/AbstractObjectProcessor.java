package org.source.spring.object.processor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.source.spring.object.AbstractValue;
import org.source.spring.object.data.ObjectFullData;
import org.source.spring.object.entity.ObjectBodyEntityIdentity;
import org.source.spring.object.entity.ObjectEntityIdentity;
import org.source.spring.object.entity.RelationEntityIdentity;
import org.source.spring.object.mapper.ObjectFullDataMapper;
import org.source.spring.trace.TraceContext;
import org.source.utility.assign.Assign;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Streams;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractObjectProcessor<O extends ObjectEntityIdentity, R extends RelationEntityIdentity> {

    public abstract O newObjectEntity();

    public abstract R newRelationEntity();

    public abstract List<O> findObjects(Collection<String> objectIds);

    public abstract Map<String, List<R>> findRelationByBelongIds(Collection<String> belongIds);

    public abstract void saveObjects(Collection<O> objects);

    public abstract void saveRelations(Collection<R> relations);

    /**
     * 数据保存处理
     */
    public <B extends ObjectBodyEntityIdentity, V extends AbstractValue> void saveObjectData(
            Collection<ObjectFullData<V>> objectData, AbstractObjectBodyProcessor<B, V> bodyProcessor) {
        if (log.isDebugEnabled()) {
            log.debug("ObjectProcessor.save, objectDataList:{}", Jsons.str(objectData));
        }
        List<O> objectList = objectData.stream().map(this::data2ObjectEntity).toList();
        List<B> objectBodyList = objectData.stream().map(bodyProcessor::data2ObjectBodyEntity).toList();
        List<R> relationList = objectData.stream().filter(k -> Objects.nonNull(k.getParentId()))
                .map(this::data2RelationEntity).toList();
        if (!CollectionUtils.isEmpty(objectList)) {
            this.saveObjects(objectList);
        }
        if (!CollectionUtils.isEmpty(objectBodyList)) {
            bodyProcessor.saveObjectBodies(objectBodyList);
        }
        if (!CollectionUtils.isEmpty(relationList)) {
            this.saveRelations(relationList);
        }
    }

    public <V extends AbstractValue> O data2ObjectEntity(ObjectFullData<V> data) {
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

    public <V extends AbstractValue> R data2RelationEntity(ObjectFullData<V> objectFullData) {
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

    public void find(Collection<String> objectIds) {
        List<ObjectTemp<O, R>> list = Streams.map(new HashSet<>(objectIds), k -> ObjectTemp.<O, R>builder().objectId(k).build()).toList();
        List<ObjectTemp<O, R>> tempList = Assign.build(list)
                .parallel()
                .addAcquire(this::findObjects, O::getObjectId)
                .addAction(ObjectTemp::getObjectId)
                .addAssemble(ObjectTemp::setObject)
                .backAcquire().backAssign()
                .addAcquire(this::findRelationByBelongIds)
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
                data.setType(object.getType());
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
        Map<Integer, AbstractObjectBodyProcessor<ObjectBodyEntityIdentity, AbstractValue>> processorMap = this.bodyProcessor();
        Map<Integer, Function<Collection<ObjectFullData<AbstractValue>>, Assign<ObjectFullData<AbstractValue>>>> assignerMap = HashMap.newHashMap(processorMap.size());
        processorMap.forEach((k, p) -> assignerMap.put(k, es ->
                Assign.build(es)
                        .addAcquire(p::findObjectBodies, ObjectBodyEntityIdentity::getObjectId)
                        .addAction(ObjectFullData::getObjectId)
                        .addAssemble((e, t) -> {
                            e.setKey(t.getKey());
                            e.setValue(p.toObjectValue(t));
                        }).backAcquire().backAssign()));
        Assign.build(objectFullData).addBranches(ObjectFullData::getType, assignerMap).invoke();
    }

    public abstract <B extends ObjectBodyEntityIdentity, V extends AbstractValue> Map<Integer, AbstractObjectBodyProcessor<B, V>> bodyProcessor();

}