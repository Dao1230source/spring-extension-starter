package org.source.spring.object.definer.processor;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.source.spring.object.BodyData;
import org.source.spring.object.ObjectDispatcher;
import org.source.spring.object.ObjectElement;
import org.source.spring.object.ObjectNode;
import org.source.spring.object.definer.entity.BodyDefiner;
import org.source.spring.object.definer.entity.ObjectDefiner;
import org.source.spring.object.definer.entity.RelationDefiner;
import org.source.spring.object.definer.enums.ObjectTypeDefiner;
import org.source.spring.object.definer.handler.BodyHandler;
import org.source.spring.object.definer.handler.ObjectTypeHandler;
import org.source.spring.object.domain.ObjectDetail;
import org.source.spring.object.enums.RelationTypeEnum;
import org.source.spring.object.enums.StatusEnum;
import org.source.spring.object.exception.ObjectExceptionEnum;
import org.source.utility.assign.Assign;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.tree.EnhanceTree;
import org.source.utility.tree.define.IdDefiner;
import org.source.utility.tree.define.Node;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Streams;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public interface BodyProcessor<
        O extends ObjectDefiner,
        B extends BodyDefiner,
        R extends RelationDefiner,
        D extends BodyData,
        T extends ObjectTypeDefiner<D>> {

    Logger log = LoggerFactory.getLogger(BodyProcessor.class);

    BodyHandler<B> getBodyHandler();

    ObjectTypeHandler<D, T> getObjectTypeHandler();

    ObjectProcessor<O, R> getObjectProcessor();

    EnhanceTree<String, ObjectElement<D>, ObjectNode<D>> getObjectTree();

    IdDefiner<String, ObjectElement<D>, ObjectNode<D>> getDataIdDefiner();

    default void register() {
        ObjectDispatcher.register(this.getObjectTypeHandler());
    }

    default @Nullable String dataId(@Nullable D d) {
        return Objects.isNull(d) ? null : d.getObjectId();
    }

    default @Nullable String parentDataId(@Nullable D d) {
        return Objects.isNull(d) ? null : d.getParentObjectId();
    }

    default @Nullable String bodyId(@Nullable B b) {
        return Objects.isNull(b) ? null : b.getObjectId();
    }

    /**
     * 查询详情关系，简单类型详情只需要查询objectBody本身即可，但复杂类型比如列表。需要查询下属的对象组合一起作为详情
     *
     * @param objectIds objectIds
     * @return 详情关系
     */
    default List<R> findDetailByObjectIds(Collection<String> objectIds) {
        return List.of();
    }

    default List<D> save(Collection<D> ds) {
        try {
            this.beforeMerge(ds);
            this.merge(ds);
            this.afterMerged();
            this.beforeSave();
            this.save();
            this.afterSave();
            return this.findFromTree(ds);
        } catch (Exception e) {
            this.getObjectTree().clear();
            log.error("AbstractObjectProcessor.save error", e);
            throw ObjectExceptionEnum.OBJECT_SAVE_ERROR.newException(e);
        } finally {
            this.after();
        }
    }

    default D save(D d) {
        List<D> save = this.save(List.of(d));
        return Streams.of(save).findFirst().orElse(null);
    }


    default void beforeMerge(Collection<D> ds) {
    }

    /**
     * 转为tree
     *
     * @param ds es
     */
    default void merge(Collection<D> ds) {
        if (log.isDebugEnabled()) {
            log.debug("source values:{}", Jsons.str(ds));
        }
        Collection<D> maybeExistsInDb = this.validExists(ds);
        if (CollectionUtils.isNotEmpty(maybeExistsInDb)) {
            if (log.isDebugEnabled()) {
                log.debug("maybeExistsInDb:{}", maybeExistsInDb);
            }
            // 从数据中查询数据并添加到tree中
            List<ObjectElement<D>> dataFromDbList = this.findFromDataToObjectElement(maybeExistsInDb);
            if (log.isDebugEnabled()) {
                log.debug("dataFromDbList:{}", dataFromDbList);
            }
            this.handleDbTree().add(dataFromDbList);
        }
        Collection<ObjectElement<D>> objectElements = Streams.map(ds, this::dataToObjectElement).toList();
        this.handleDataTree().add(objectElements);
        this.afterMerged();
    }

    /**
     * 需要校验是否在数据库是否存在的V
     *
     * @param ds vs
     * @return vs
     */
    default List<D> validExists(Collection<D> ds) {
        return Streams.retain(ds, d -> {
            String dataId = this.dataId(d);
            return StringUtils.isEmpty(dataId) ||
                    this.getObjectTree().findByKey(dataId).map(n -> !StatusEnum.DATABASE.equals(n.getStatus())).isPresent();
        }).toList();
    }


    default void afterMerged() {
    }

    /**
     * 对 this.getDocTree() 做一些操作
     */
    default void beforeSave() {
    }

    /**
     * 持久化数据
     */
    default void save() {
        List<ObjectNode<D>> objectNodes = this.filterToSaveObjectData();
        if (CollectionUtils.isEmpty(objectNodes)) {
            return;
        }
        List<O> objectList = new ArrayList<>(objectNodes.size());
        List<B> objectBodyList = new ArrayList<>(objectNodes.size());
        List<R> relationList = new ArrayList<>(objectNodes.size());
        objectNodes.stream().filter(n -> Objects.nonNull(n.getElement())).forEach(n -> {
            if (log.isDebugEnabled()) {
                log.debug("nodeConvertToEntities, objectNodes:{}", Jsons.str(n.getElement()));
            }
            if (StatusEnum.updateObject(n.getStatus())) {
                objectList.add(this.getObjectFromNode(n));
            }
            if (StatusEnum.updateObjectBody(n.getStatus())) {
                objectBodyList.add(this.getBodyFromNode(n));
            }
            if (StatusEnum.updateRelation(n.getStatus())) {
                relationList.addAll(this.getRelationsFromNode(n));
            }
        });
        this.saveObjectData(objectList, objectBodyList, relationList);
    }

    default List<ObjectNode<D>> filterToSaveObjectData() {
        return this.getObjectTree().getIdMap().values().stream()
                .filter(n -> !StatusEnum.DATABASE.equals(n.getStatus()))
                .toList();
    }


    @Builder
    @Data
    class FindObjectDetailTemp<O, B, D> {
        private D sourceData;
        private @Nullable String objectId;
        private @Nullable B body;
        private @Nullable O object;
        private @Nullable String parentObjectId;
        private @Nullable B parentBody;
        private @Nullable O parentObject;
    }

    /**
     * ObjectElement 的key批量查询
     *
     * @param ds vs
     * @return {@literal Collection<ObjectElement<V>>}
     */
    default List<ObjectElement<D>> findFromDataToObjectElement(Collection<D> ds) {
        return Assign.build(ds)
                .cast(d -> FindObjectDetailTemp.<O, B, D>builder().sourceData(d).build())
                .name("get body by dataIds")
                // 查询 objectBody
                .addAcquire(this.getBodyHandler()::findObjectBodyByDataIds, this::bodyId)
                .name("select body by dataIds").throwException()
                .addAction(t -> this.dataId(t.getSourceData()))
                .name("by dataIds")
                .addAssemble((e, t) -> {
                    e.setBody(t);
                    e.setObjectId(t.getObjectId());
                })
                .backAcquire()
                .addAction(k -> this.parentDataId(k.getSourceData()))
                .name("by parentDataIds")
                .addAssemble((e, t) -> {
                    e.setParentBody(t);
                    e.setParentObjectId(t.getObjectId());
                })
                .backAcquire().backAssign()
                // 获取 object 数据依赖于先获取到 objectBody 数据
                .dependBy()
                .name("get object by objectIds")
                .addAcquire(this.getObjectProcessor().getObjectHandler()::find, O::getObjectId)
                .name("for objectIds").throwException()
                .addAction(FindObjectDetailTemp::getObjectId)
                .addAssemble(FindObjectDetailTemp::setObject)
                .backAcquire()
                .addAction(FindObjectDetailTemp::getParentObjectId)
                .addAssemble(FindObjectDetailTemp::setParentObject)
                .backAcquire().backAssign()
                // 获取 relation 数据依赖于先获取到 object 数据
                .dependBy()
                // 必须同时获取到 object 和 body 才能转换为 data
                .casts(ts -> ts.stream().map(t -> {
                    List<ObjectDetail<O, R, D>> objectDetails = new ArrayList<>(2);
                    if (StringUtils.isNotEmpty(t.getObjectId()) && Objects.nonNull(t.getObject()) && Objects.nonNull(t.getBody())) {
                        D d = this.bodyToData(t.getObject(), t.getBody());
                        ObjectDetail<O, R, D> detail = ObjectDetail.<O, R, D>builder().objectId(t.getObjectId()).object(t.getObject()).data(d).build();
                        if (StringUtils.isNotEmpty(t.getParentObjectId())) {
                            R r = this.newRelation();
                            r.setObjectId(t.getObjectId());
                            r.setParentObjectId(t.getParentObjectId());
                            r.setType(RelationTypeEnum.LINKS.getType());
                            r.setSorted("1");
                            detail.setRelation(r);
                        }
                        objectDetails.add(detail);
                    }
                    if (StringUtils.isNotEmpty(t.getParentObjectId()) && Objects.nonNull(t.getParentObject()) && Objects.nonNull(t.getParentBody())) {
                        D d = this.bodyToData(t.getParentObject(), t.getParentBody());
                        ObjectDetail<O, R, D> detail = ObjectDetail.<O, R, D>builder().objectId(t.getParentObjectId()).object(t.getParentObject()).data(d).build();
                        objectDetails.add(detail);
                    }
                    return objectDetails;
                }).flatMap(Collection::stream).toList())
                .name("cast to ObjectDetail")
                .cast(ObjectDetail::toObjectElement)
                .name("cast to objectElement")
                // 最终执行
                .invoke()
                .toList();
    }

    default D bodyToData(O object, B body) {
        ObjectTypeDefiner<D> objectType = ObjectDispatcher.getByType(object.getType());
        D d = Jsons.obj(body.getValue(), objectType.getDataClass());
        d.setObjectId(object.getObjectId());
        d.setName(body.getName());
        return d;
    }


    /**
     * tree 保存数据库的数据
     *
     * @return tree
     */
    default EnhanceTree<String, ObjectElement<D>, ObjectNode<D>> handleDbTree() {
        EnhanceTree<String, ObjectElement<D>, ObjectNode<D>> tree = this.getObjectTree();
        // 默认 ObjectElement.getId() 作为 tree 关联的 id
        tree.setIdDefiner(IdDefiner.defaultIdExtend());
        tree.setAfterCreateHandler(n -> n.setStatus(StatusEnum.DATABASE));
        // 另以指定的 dataId 为唯一键缓存，以便新的 data 对象加入时可以通过自定义的id找到缓存的Node
        tree.setAdditionalIdDefiners(List.of(this.getDataIdDefiner()));
        tree.setMergeHandler(this::mergeNode);
        return tree;
    }

    default ObjectElement<D> dataToObjectElement(D data) {
        ObjectElement<D> objectElement = new ObjectElement<>();
        objectElement.setType(ObjectDispatcher.getByClass(data.getClass()).getType());
        objectElement.setData(data);
        this.dataToObjectElementAdditional(objectElement, data);
        return objectElement;
    }

    default void dataToObjectElementAdditional(ObjectElement<D> objectElement, D data) {
    }

    default EnhanceTree<String, ObjectElement<D>, ObjectNode<D>> handleDataTree() {
        EnhanceTree<String, ObjectElement<D>, ObjectNode<D>> tree = this.getObjectTree();
        // 如果没有指定 objectId，设置为 Uid
        tree.setAfterCreateHandler(n -> {
            n.setStatus(StatusEnum.CREATED);
            if (Objects.isNull(n.getId())) {
                Node.setProperty(n, e -> e.getData().setObjectId(this.getObjectId()));
            }
        });
        // 新的 data 对象加入时还未生成id，所以指定业务 dataId 作为 tree 关联的 id
        tree.setIdDefiner(this.getDataIdDefiner());
        tree.setMergeHandler(this::mergeNode);
        return tree;
    }

    default ObjectNode<D> mergeNode(ObjectNode<D> n, @Nullable ObjectNode<D> old) {
        log.debug("new object id:{}, status:{}", n.getId(), n.getStatus());
        if (Objects.isNull(old)) {
            // 默认CREATED
            n.setStatus(Objects.requireNonNullElse(n.getStatus(), StatusEnum.CREATED));
            return n;
        }
        log.debug("old object id:{}, status:{}", old.getId(), old.getStatus());
        // 现有的node已保存到数据库
        if (StatusEnum.DATABASE.equals(old.getStatus()) || StatusEnum.DATABASE.equals(n.getStatus())) {
            // 如果新的element与数据库的不相同，更新
            if (this.nodeNotEquals(n, old)) {
                log.debug("new object not from database and not equal old object.new:{} old:{}", Jsons.str(n), Jsons.str(old));
                ObjectElement<D> newElement = n.getElementOrElseThrow();
                ObjectElement<D> oldElement = old.getElementOrElseThrow();
                newElement.getData().setObjectId(oldElement.getId());
                D d = this.mergeData(newElement.getData(), oldElement.getData());
                oldElement.setData(d);
                old.setStatus(StatusEnum.CACHED_OBJECT_BODY);
            }
            return old;
        }
        // 数据库的objectId
        old.setStatus(StatusEnum.CACHED);
        return old;
    }

    default boolean nodeNotEquals(ObjectNode<D> n, ObjectNode<D> old) {
        return n.equals(old);
    }

    /**
     * 当数据库已存在相同objectId的记录，但新的数据与数据库不相同时，如何合并数据
     *
     * @param old may be used for further computation in overriding classes
     */
    default D mergeData(D d, D old) {
        return d;
    }

    default O newObject() {
        return this.getObjectProcessor().getObjectHandler().newObject();
    }

    default B newBody() {
        return this.getBodyHandler().newObjectBodyEntity();
    }

    default R newRelation() {
        return this.getObjectProcessor().getRelationHandler().newRelation();
    }

    default String getObjectId() {
        return this.getObjectProcessor().getObjectId();
    }

    default String getSpaceId() {
        return this.getObjectProcessor().getSpaceId();
    }

    default String getUserId() {
        return this.getObjectProcessor().getUserId();
    }

    default O getObjectFromNode(ObjectNode<D> node) {
        O o = newObject();
        ObjectElement<D> element = node.getElementOrElseThrow();
        o.setSpaceId(this.getSpaceId());
        o.setDeleted(Boolean.FALSE);
        o.setObjectId(element.getId());
        o.setType(element.getType());
        this.objectAdditional(o, element);
        return o;
    }

    /**
     * @param entity  扩展使用
     * @param element 扩展使用
     */
    default void objectAdditional(O entity, ObjectElement<D> element) {
    }

    default B getBodyFromNode(ObjectNode<D> node) {
        B objectBody = this.newBody();
        ObjectElement<D> element = node.getElementOrElseThrow();
        objectBody.setCreateUser(this.getUserId());
        objectBody.setCreateTime(LocalDateTime.now());
        objectBody.setUpdateUser(this.getUserId());
        objectBody.setUpdateTime(LocalDateTime.now());
        objectBody.setObjectId(element.getId());
        objectBody.setName(element.getData().getName());
        objectBody.setValue(Jsons.str(element.getData()));
        this.bodyAdditional(objectBody, element);
        return objectBody;
    }

    default void bodyAdditional(B entity, ObjectElement<D> element) {
    }

    default List<R> getRelationsFromNode(ObjectNode<D> node) {
        ObjectElement<D> element = node.getElementOrElseThrow();
        return node.findParents().stream().filter(p -> Objects.nonNull(p.getElement())).map(p -> {
            String parentObjectId = p.getElement().getId();
            R relation = this.newRelation();
            relation.setCreateUser(this.getUserId());
            relation.setCreateTime(LocalDateTime.now());
            relation.setType(Objects.requireNonNullElse(element.getData().getRelationType(), RelationTypeEnum.LINKS.getType()));
            relation.setParentObjectId(parentObjectId);
            relation.setObjectId(element.getId());
            relation.setSorted(element.getData().getSorted());
            this.relationAdditional(relation, element);
            return relation;
        }).toList();
    }

    default void relationAdditional(R entity, ObjectElement<D> element) {
    }

    /**
     * 数据保存处理
     */
    default void saveObjectData(Collection<O> objectList, Collection<B> objectBodyList, Collection<R> relationList) {
        if (!CollectionUtils.isEmpty(objectList)) {
            this.getObjectProcessor().getObjectHandler().save(objectList);
        }
        if (!CollectionUtils.isEmpty(objectBodyList)) {
            this.getBodyHandler().save(objectBodyList);
        }
        if (!CollectionUtils.isEmpty(relationList)) {
            this.getObjectProcessor().getRelationHandler().saveRelations(relationList);
        }
    }

    default void afterSave() {
        this.getObjectTree().forEach((i, n) -> n.setStatus(StatusEnum.DATABASE));
    }

    default List<D> findFromTree(Collection<D> ds) {
        List<String> keys = Streams.of(ds).map(this::dataId).toList();
        return this.getObjectTree().findByKeys(keys).stream()
                .map(ObjectNode::getElement)
                .filter(Objects::nonNull).map(ObjectElement::getData)
                .toList();
    }

    default void after() {
        this.getObjectTree().clear();
    }

    default Assign<ObjectDetail<O, R, D>> assignBody(Collection<ObjectDetail<O, R, D>> details) {
        return Assign.build(details)
                .name("查询body")
                .peek(k -> BaseExceptionEnum.NOT_NULL.nonNull(k.getObject(), "object must not null when assign body"))
                .addAcquire(ks -> this.getBodyHandler().find(ks), B::getObjectId)
                .name("查询body，如果data已经存在，不查询").throwException()
                .addAction(k -> Objects.nonNull(k.getData()) ? null : k.getObjectId())
                .addAssemble((e, t) -> {
                    D d = this.bodyToData(Objects.requireNonNull(e.getObject()), t);
                    e.setData(d);
                })
                .backAcquire().backAssign();
    }

    default Assign<ObjectDetail<O, R, D>> removeBody(Collection<ObjectDetail<O, R, D>> details) {
        return Assign.build(details)
                .name("移除body")
                .peek(k -> BaseExceptionEnum.NOT_NULL.nonNull(k.getObject(), "object must not null when assign body"))
                .addSub(ds -> {
                    List<String> objectIds = Streams.map(ds, ObjectDetail::getObjectId).toList();
                    this.getBodyHandler().removeByObjectIds(objectIds);
                });
    }

    default Assign<ObjectDetail<O, R, D>> assignDetailRelations(Collection<ObjectDetail<O, R, D>> details) {
        return Assign.build(details)
                .name("查询详情关系")
                .peek(k -> BaseExceptionEnum.NOT_NULL.notEmpty(k.getObjectId(), "objectId must not empty when assign detail relations"))
                .addAcquireOutGroup(this::findDetailByObjectIds, R::getParentObjectId)
                .name("通过objectIds 获取详情的 relations")
                .addAction(ObjectDetail::getObjectId)
                .addAssemble(ObjectDetail::setByParentObjectIdRelations)
                .backAcquire().backAssign();
    }

}
