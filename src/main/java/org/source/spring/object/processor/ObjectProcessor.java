package org.source.spring.object.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.source.spring.object.AbstractValue;
import org.source.spring.object.StatusEnum;
import org.source.spring.object.data.ObjectData;
import org.source.spring.object.entity.ObjectEntity;
import org.source.spring.object.entity.RelationEntity;
import org.source.spring.object.tree.ObjectNode;
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
import java.util.function.BinaryOperator;

public interface ObjectProcessor<O extends ObjectEntity, R extends RelationEntity, V extends AbstractValue> {
    Logger log = LoggerFactory.getLogger(ObjectProcessor.class);

    @NonNull
    O newObjectEntity();

    @NonNull
    R newRelationEntity();

    Tree<String, V, ObjectNode<String, V>> getDocTree();

    /**
     * 转为tree
     *
     * @param vs es
     */
    default void transfer2tree(Collection<V> vs) {
        Collection<V> notExistsDb = this.notExistsDb(vs);
        if (!CollectionUtils.isEmpty(notExistsDb)) {
            // 从数据中查询数据并添加到tree中
            List<V> dataFromDbList = this.findFromDb(notExistsDb).stream().map(this::valueFromObject).toList();
            // 如果相同 key 的数据已存在，更新 objectId
            this.getDocTree().add(dataFromDbList,
                    true,
                    n -> n.setStatus(StatusEnum.DATABASE),
                    this.getUpdateOldHandler(),
                    null);
        }
        this.getDocTree().add(vs, false, n -> n.setStatus(StatusEnum.CREATED),
                this.getUpdateOldHandler(), null);
        this.afterTransfer();
    }

    default void afterTransfer() {
    }

    /**
     * 持久化数据
     *
     * @param relationIdentity 关系定义
     */
    default void persist2Database(RelationIdentity relationIdentity) {
        this.beforePersist();
        this.saveObjectData(this.obtainObjectData(), relationIdentity);
        this.afterPersist();
    }

    /**
     * 对 this.getDocTree() 做一些操作
     */
    default void beforePersist() {
        this.getDocTree().forEach((i, n) -> {
            if (StatusEnum.DATABASE.equals(n.getOldStatus())
                    && Objects.nonNull(n.getOldElement())
                    && n.getOldElement().equals(n.getElement())) {
                n.setStatus(StatusEnum.DATABASE);
            }
        });
    }

    default List<ObjectData<V>> obtainObjectData() {
        return this.getDocTree().getIdMap().values().stream()
                .filter(k -> !StatusEnum.DATABASE.equals(k.getStatus()))
                .map(AbstractNode::getElement).filter(Objects::nonNull).map(this::convert2Object).toList();
    }

    default void afterPersist() {
        this.getDocTree().forEach((i, n) -> n.setStatus(StatusEnum.DATABASE));
    }

    default BinaryOperator<ObjectNode<String, V>> getUpdateOldHandler() {
        return (n, old) -> {
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
                    old.setElement(n.getElement());
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
        };
    }

    default Collection<V> notExistsDb(Collection<V> vs) {
        List<V> vsFromDb = this.getDocTree().find(n -> StatusEnum.DATABASE.equals(n.getStatus()))
                .stream().map(AbstractNode::getElement).filter(Objects::nonNull).toList();
        return Streams.notRetain(vs, AbstractValue::getId, vsFromDb).toList();
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

    default R data2RefEntity(ObjectData<V> objectData, RelationIdentity relationIdentity) {
        R relationEntity = this.newRelationEntity();
        relationEntity.setObjectId(objectData.getObjectId());
        relationEntity.setParentObjectId(objectData.getParentObjectId());
        relationEntity.setType(relationIdentity.getType());
        relationEntity.setCreateUser(TraceContext.getUserIdOrDefault());
        return relationEntity;
    }

    default ObjectIdentity getObjectHandler(V v) {
        return new DefaultObjectIdentity();
    }

    default ObjectData<V> convert2Object(V objectValue) {
        ObjectData<V> objectData = new ObjectData<>();
        objectData.setObjectId(objectValue.getObjectId());
        ObjectIdentity objectIdentity = this.getObjectHandler(objectValue);
        BaseExceptionEnum.NOT_NULL.nonNull(objectIdentity,
                "get ObjectHandler is null by {}", objectValue.getClass().getName());
        objectData.setType(objectIdentity.getType());
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
    default void saveObjectData(Collection<ObjectData<V>> objectData, RelationIdentity relationIdentity) {
        if (log.isDebugEnabled()) {
            log.debug("ObjectProcessor.save, objectDataList:{}", Jsons.str(objectData));
        }
        List<O> objectList = objectData.stream().map(this::data2entity).toList();
        List<R> relationList = objectData.stream().filter(k -> Objects.nonNull(k.getParentId()))
                .map(k -> this.data2RefEntity(k, relationIdentity)).toList();
        if (!CollectionUtils.isEmpty(objectList)) {
            this.saveObjects(objectList);
        }
        if (!CollectionUtils.isEmpty(relationList)) {
            this.saveRelations(relationList);
        }
    }
}