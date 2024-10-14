package org.source.spring.object.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.source.spring.object.ObjectStatusEnum;
import org.source.spring.object.ObjectValueElement;
import org.source.spring.object.data.ObjectData;
import org.source.spring.object.entity.ObjectEntity;
import org.source.spring.object.entity.RelationEntity;
import org.source.spring.trace.TraceContext;
import org.source.spring.uid.Ids;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.tree.Tree;
import org.source.utility.tree.identity.AbstractNode;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Streams;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public interface ObjectProcessor<O extends ObjectEntity, R extends RelationEntity,
        V extends ObjectValueElement, N extends AbstractNode<String, V, N>> {
    Logger log = LoggerFactory.getLogger(ObjectProcessor.class);

    @NonNull
    O newObjectEntity();

    @NonNull
    R newRelationEntity();

    Tree<String, V, N> getDocTree();

    default BiConsumer<N, N> getUpdateOldHandler() {
        return (n, old) -> {
            // 数据库的objectId
            if (ObjectStatusEnum.DATABASE.equals(n.getElement().getObjectStatus())) {
                old.getElement().setObjectId(n.getElement().getObjectId());
            }
            old.getElement().setObjectStatus(ObjectStatusEnum.CACHED);
        };
    }

    default Collection<V> maybeFromDb(Collection<V> es) {
        List<N> nodes = this.getDocTree().find(n -> !ObjectStatusEnum.DATABASE.equals(n.getElement().getObjectStatus()));
        return Streams.map(nodes, AbstractNode::getElement).toList();
    }

    /**
     * ObjectData 的key批量查询
     *
     * @param vs es
     * @return {@literal Collection<ObjectData>}
     */
    @NonNull
    default Collection<ObjectData<V>> findFromDb(Collection<V> vs) {
        return List.of();
    }

    default @Nullable V valueFromObject(ObjectData<V> objectData) {
        return objectData.getValue();
    }

    default void saveObjects(Collection<O> objects) {

    }

    default void saveRelations(Collection<R> relations) {

    }

    default O data2entity(ObjectData<V> data) {
        O objectEntity = this.newObjectEntity();
        objectEntity.setObjectId(data.getObjectId());
        objectEntity.setKey(data.getKey());
        objectEntity.setValue(Jsons.str(data.getValue()));
        objectEntity.setType(data.getType());
        objectEntity.setDeleted(Boolean.FALSE);
        objectEntity.setCreateUser(TraceContext.getUserIdOrDefault());
        objectEntity.setUpdateUser(TraceContext.getUserIdOrDefault());
        return objectEntity;
    }

    default R data2RefEntity(ObjectData<V> objectData, RelationHandler relationHandler) {
        R relationEntity = this.newRelationEntity();
        relationEntity.setObjectId(objectData.getObjectId());
        relationEntity.setParentObjectId(objectData.getParentObjectId());
        relationEntity.setType(relationHandler.getType());
        relationEntity.setCreateUser(TraceContext.getUserIdOrDefault());
        return relationEntity;
    }

    default ObjectHandler getObjectHandler(V v) {
        return new DefaultObjectHandler();
    }

    /**
     * AbstractDoclet 中使用，转为tree结构，保存在内存中
     *
     * @param vs es
     */
    default void add2Tree(Collection<V> vs) {
        vs.forEach(k -> k.setObjectStatus(ObjectStatusEnum.CREATED));
        this.sync2tree(vs);
    }

    default void sync2tree(Collection<V> vs) {
        this.getDocTree().add(vs);
        Collection<V> maybeFromDbValues = this.maybeFromDb(vs);
        if (CollectionUtils.isEmpty(maybeFromDbValues)) {
            return;
        }
        // 从数据中查询数据并添加到tree中
        List<V> dataFromDbList = this.findFromDb(maybeFromDbValues).stream().map(k -> {
            V objectValue = this.valueFromObject(k);
            if (Objects.nonNull(objectValue)) {
                objectValue.setObjectStatus(ObjectStatusEnum.DATABASE);
            }
            return objectValue;
        }).toList();
        // 如果相同 key 的数据已存在，更新 objectId
        this.getDocTree().add(dataFromDbList, this.getUpdateOldHandler());
    }

    default ObjectData<V> convert2Object(V objectValue) {
        ObjectData<V> objectData = new ObjectData<>();
        objectData.setObjectId(objectValue.getObjectId());
        ObjectHandler objectHandler = this.getObjectHandler(objectValue);
        BaseExceptionEnum.NOT_NULL.nonNull(objectHandler,
                "get ObjectHandler is null by {}", objectValue.getClass().getName());
        objectData.setType(objectHandler.getType());
        objectData.setValue(objectValue);
        // 如果objectId为null，表明是新增数据
        if (Objects.isNull(objectValue.getObjectId())) {
            objectData.setObjectId(Ids.stringId());
            objectValue.setObjectId(objectData.getObjectId());
        }
        return objectData;
    }

    /**
     * 数据保存处理
     */
    default void saveObjectData(Collection<ObjectData<V>> objectData, RelationHandler relationHandler) {
        if (log.isDebugEnabled()) {
            log.debug("ObjectProcessor.save, objectDataList:{}", Jsons.str(objectData));
        }
        List<O> objectList = objectData.stream().map(this::data2entity).toList();
        List<R> relationList = objectData.stream().filter(k -> Objects.nonNull(k.getParentId()))
                .map(k -> this.data2RefEntity(k, relationHandler)).toList();
        if (!CollectionUtils.isEmpty(objectList)) {
            this.saveObjects(objectList);
        }
        if (!CollectionUtils.isEmpty(relationList)) {
            this.saveRelations(relationList);
        }
    }
}
