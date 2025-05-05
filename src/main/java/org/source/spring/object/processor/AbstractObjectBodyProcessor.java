package org.source.spring.object.processor;

import lombok.extern.slf4j.Slf4j;
import org.source.spring.object.AbstractValue;
import org.source.spring.object.StatusEnum;
import org.source.spring.object.data.ObjectFullData;
import org.source.spring.object.entity.ObjectBodyEntityIdentity;
import org.source.spring.object.tree.ObjectNode;
import org.source.spring.trace.TraceContext;
import org.source.spring.uid.Ids;
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

@Slf4j
public abstract class AbstractObjectBodyProcessor<B extends ObjectBodyEntityIdentity, V extends AbstractValue> {
    private final AbstractObjectProcessor<?, ?> objectProcessor;

    protected AbstractObjectBodyProcessor(AbstractObjectProcessor<?, ?> objectProcessor) {
        this.objectProcessor = objectProcessor;
    }

    public abstract Tree<String, V, ObjectNode<String, V>> getDocTree();

    public abstract B newObjectBodyEntity();

    public abstract List<B> findObjectBodies(Collection<String> referenceIds);

    public abstract void saveObjectBodies(Collection<B> objectBodies);

    public abstract Integer getObjectType(V v);

    public abstract V toObjectValue(B objectBodyEntity);

    /**
     * 转为tree
     *
     * @param vs es
     */
    public void transfer2tree(Collection<V> vs) {
        Collection<V> notExistsDb = this.maybeExistsDb(vs);
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

    public void afterTransfer() {
    }

    /**
     * 持久化数据
     */
    public void persist2Database() {
        this.beforePersist();
        List<ObjectFullData<V>> objectFullData = this.obtainObjectData();
        this.objectProcessor.saveObjectData(objectFullData, this);
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

    public BinaryOperator<ObjectNode<String, V>> getUpdateOldHandler() {
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

    public Collection<V> maybeExistsDb(Collection<V> vs) {
        List<V> vsFromDb = this.getDocTree().find(n -> StatusEnum.DATABASE.equals(n.getStatus()))
                .stream().map(AbstractNode::getElement).filter(Objects::nonNull).toList();
        return Streams.notRetain(vs, AbstractValue::getId, vsFromDb).toList();
    }

    /**
     * ObjectFullData 的key批量查询
     *
     * @param vs es
     * @return {@literal Collection<ObjectFullData>}
     */
    @NonNull
    public Collection<ObjectFullData<V>> findFromDb(Collection<V> vs) {
        return List.of();
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

    public B data2ObjectBodyEntity(ObjectFullData<V> data) {
        B entity = this.newObjectBodyEntity();
        if (Objects.isNull(entity)) {
            return null;
        }
        entity.setObjectId(data.getObjectId());
        entity.setKey(data.getKey());
        entity.setValue(Jsons.str(data.getValue()));
        entity.setCreateUser(TraceContext.getUserIdOrDefault());
        entity.setUpdateUser(TraceContext.getUserIdOrDefault());
        return entity;
    }
}