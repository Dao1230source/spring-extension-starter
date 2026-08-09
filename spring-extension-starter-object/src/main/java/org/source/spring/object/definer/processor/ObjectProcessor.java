package org.source.spring.object.definer.processor;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.source.spring.object.BodyData;
import org.source.spring.object.ObjectDispatcher;
import org.source.spring.object.ObjectElement;
import org.source.spring.object.ObjectNode;
import org.source.spring.object.definer.entity.BodyDefiner;
import org.source.spring.object.definer.entity.ObjectDefiner;
import org.source.spring.object.definer.entity.RelationDefiner;
import org.source.spring.object.definer.enums.ObjectTypeDefiner;
import org.source.spring.object.definer.handler.ObjectHandler;
import org.source.spring.object.definer.handler.RelationHandler;
import org.source.spring.object.domain.ObjectDetail;
import org.source.spring.object.domain.RelateResult;
import org.source.spring.object.domain.RelationKey;
import org.source.spring.object.domain.Relations;
import org.source.utility.assign.Assign;
import org.source.utility.assign.InterruptStrategyEnum;
import org.source.utility.tree.EnhanceTree;
import org.source.utility.utils.Streams;
import org.source.utility.utils.Strings;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 *
 * @param <O> object
 * @param <R> relation
 */
public interface ObjectProcessor<O extends ObjectDefiner, R extends RelationDefiner> {

    ObjectHandler<O> getObjectHandler();

    RelationHandler<R> getRelationHandler();

    String getObjectId();

    String getSpaceId();

    String getUserId();

    default <D extends BodyData> ObjectNode<D> find(Collection<String> objectIds) {
        Collection<ObjectElement<D>> fullData = this.<D>assignObjectAndBody(objectIds)
                .addAcquireOutGroup(ks -> this.getRelationHandler().findByObjectIds(ks), R::getObjectId)
                .name("获取与父级关联的relation数据")
                .addAction(ObjectDetail::getObjectId)
                .addAssemble((e, rs) -> {
                    e.setByObjectIdRelations(rs);
                    Streams.of(rs).filter(r -> objectIds.contains(r.getParentObjectId())).findFirst().ifPresent(e::setRelation);
                })
                .backAcquire().backAssign().invoke()
                .cast(ObjectDetail::toObjectElement)
                .toList();
        // 组成 tree 结构
        return EnhanceTree.of(new ObjectNode<D>()).add(fullData);
    }


    default <D extends BodyData> Assign<ObjectDetail<O, R, D>> assignObjectAndBody(Collection<String> objectIds) {
        Assign<ObjectDetail<O, R, D>> objectDetailAssign = this.assign(objectIds);
        return objectDetailAssign.andThen(this::assignObjectAndBody);
    }

    default <D extends BodyData> Assign<ObjectDetail<O, R, D>> assignObjectAndBody(Assign<ObjectDetail<O, R, D>> objectIdAssign) {
        return objectIdAssign.andThen(this::assignObject).invoke().andThen(this::assignBody);
    }

    default <D extends BodyData> Assign<ObjectDetail<O, R, D>> assign(Collection<String> objectIds) {
        return Assign.build(objectIds)
                // objectId 转为 ObjectTemp
                .cast(e -> ObjectDetail.<O, R, D>builder().objectId(e).build())
                .name("通过objectIds获取数据").parallel().interruptStrategy(InterruptStrategyEnum.ANY);
    }

    /**
     * ObjectDetail.objectId 不能为空
     *
     * @param objectIdAssign objectIdAssign
     * @return objectIdAssign find object and body
     */
    default <D extends BodyData> Assign<ObjectDetail<O, R, D>> assignObject(Assign<ObjectDetail<O, R, D>> objectIdAssign) {
        return objectIdAssign
                // 查询 object
                .addAcquire(this.getObjectHandler()::find, O::getObjectId)
                .name("获取object数据").throwException()
                .addAction(k -> Objects.nonNull(k.getObject()) ? null : k.getObjectId())
                .addAssemble(ObjectDetail::setObject)
                .backAcquire().backAssign();
    }

    default <D extends BodyData> Assign<ObjectDetail<O, R, D>> assignBody(Assign<ObjectDetail<O, R, D>> objectAssign) {
        return objectAssign
                // 依赖上一个 assign
                .dependBy()
                .name("获取body")
                .addBranches(k -> Objects.requireNonNull(k.getObject()).getType(), (k, es) -> {
                    BodyProcessor<O, BodyDefiner, R, D, ObjectTypeDefiner<D>> processor = ObjectDispatcher.getProcessorByType(k);
                    return processor.assignBody(es);
                });
    }

    default <D extends BodyData> Assign<ObjectDetail<O, R, D>> assignDetailRelations(Assign<ObjectDetail<O, R, D>> detailAssign) {
        return detailAssign
                // 依赖上一个 assign
                .dependBy()
                .name("获取详情关系")
                .addBranches(k -> Objects.requireNonNull(k.getObject()).getType(), (k, es) -> {
                    BodyProcessor<O, BodyDefiner, R, D, ObjectTypeDefiner<D>> processor = ObjectDispatcher.getProcessorByType(k);
                    return processor.assignDetailRelations(es);
                });
    }


    default <D extends BodyData> List<ObjectNode<D>> getDetail(Collection<String> objectIds) {
        if (CollectionUtils.isEmpty(objectIds)) {
            return List.of();
        }
        List<ObjectElement<D>> elements = this.<D>assignObjectAndBody(objectIds).andThen(this::assignDetailRelations).invoke()
                .casts(ks -> Streams.of(ks).map(k -> {
                    List<ObjectDetail<O, R, D>> details = new ArrayList<>();
                    details.add(k);
                    if (CollectionUtils.isNotEmpty(k.getByParentObjectIdRelations())) {
                        k.getByParentObjectIdRelations().forEach(r -> {
                            ObjectDetail<O, R, D> detail = ObjectDetail.<O, R, D>builder().objectId(r.getObjectId()).relation(r).build();
                            details.add(detail);
                        });
                    }
                    return details;
                }).flatMap(Collection::stream).toList())
                // 继续补充获取其他关联的object和body
                .andThen(this::assignObjectAndBody).invoke()
                .cast(ObjectDetail::toObjectElement)
                .toList();
        // 组成 tree 结构
        EnhanceTree<String, ObjectElement<D>, ObjectNode<D>> tree = EnhanceTree.of(new ObjectNode<>());
        tree.add(elements);
        return tree.findByKeys(objectIds);
    }

    default ObjectNode<BodyData> getDetail(String objectId) {
        return getDetail(List.of(objectId)).stream().findFirst().orElse(null);
    }

    /**
     * 如果有不能关联的关系，返回，不操作其他数据
     *
     * @param relates Relations
     * @return 不能关联的数据
     */
    default List<RelateResult<R>> relate(Collection<Relations> relates) {
        if (CollectionUtils.isEmpty(relates)) {
            return List.of();
        }
        List<RelateResult<R>> relateResultList = Assign.build(relates)
                .cast(RelateResult::<R>of)
                .peek(r -> {
                    r.setCanRelate(true);
                    if (Objects.isNull(r.getType())) {
                        r.setType(RelationDefiner.DEFAULT_TYPE);
                    }
                    if (StringUtils.isEmpty(r.getSort())) {
                        r.setSort(RelationDefiner.DEFAULT_SORT);
                    }
                })
                .name("校验relation是否存在")
                .addAcquire(this.getRelationHandler()::findByRelateUniqueKeys, RelationKey::of)
                .addAction(RelationKey::of)
                .backAcquire()
                .afterProcessor((e, kt) -> {
                    R r = kt.get(RelationKey.of(e));
                    if (e.isAdd()) {
                        if (Objects.nonNull(r)) {
                            e.setCanRelate(false);
                            e.setRelateResult(Strings.format("已存在关联关系：{}，不可重复新增", e.toPlainString()));
                        } else {
                            e.setRelation(this.relateDataToRelation(e));
                        }
                    }
                    if (e.isDelete()) {
                        if (Objects.isNull(r)) {
                            e.setRelateResult(Strings.format("不存在关联关系：{}，无需删除", e.toPlainString()));
                        } else {
                            e.setRelation(r);
                        }
                    }
                }).backAssign()
                .dependBy()
                .name("校验relation是否相互循环")
                .addBranch(k -> k.isAdd() && k.isCanRelate())
                .name("只校验新增且可关联的数据")
                .addAcquire(this.getRelationHandler()::findByRelateUniqueKeys, RelationKey::of)
                .addAction(k -> RelationKey.of(k).reversed())
                .backAcquire()
                .afterProcessor((e, kt) -> {
                    RelationKey key = RelationKey.of(e);
                    RelationKey reversedKey = key.reversed();
                    R r = kt.get(reversedKey);
                    if (Objects.nonNull(r)) {
                        e.setRelateResult(Strings.format("已存在关联关系：{},不可相互循环关联：{}", reversedKey.toPlainString(), key.toPlainString()));
                    }
                }).backAssign()
                .backUppermost()
                .invoke().toList();
        // 如果有不能关联的，返回
        List<RelateResult<R>> cannotRelateList = Streams.retain(relateResultList, k -> !k.isCanRelate()).toList();
        if (CollectionUtils.isNotEmpty(cannotRelateList)) {
            return cannotRelateList;
        }
        List<R> toAddList = Streams.retain(relateResultList, k -> k.isAdd() && k.isCanRelate()).map(this::relateDataToRelation).toList();
        if (CollectionUtils.isNotEmpty(toAddList)) {
            this.getRelationHandler().saveRelations(toAddList);
        }
        List<Long> toDeleteList = Streams.retain(relateResultList, k -> k.isDelete() && k.isCanRelate())
                .map(RelateResult::getRelation).map(R::getId).toList();
        if (CollectionUtils.isNotEmpty(toDeleteList)) {
            this.getRelationHandler().removeByRelationIds(toDeleteList);
        }
        return List.of();
    }

    default R relateDataToRelation(Relations relations) {
        R relation = this.getRelationHandler().newRelation();
        relation.setParentObjectId(relations.getParentObjectId());
        relation.setObjectId(relations.getObjectId());
        relation.setType(relations.getType());
        relation.setSort(relations.getSort());
        relation.setCreateUser(this.getUserId());
        relation.setCreateTime(LocalDateTime.now());
        return relation;
    }

    default void delete(Collection<String> objectIds) {
        this.getObjectHandler().delete(objectIds);
    }

    default void remove(Collection<String> objectIds) {
        this.getObjectHandler().remove(objectIds);
        this.getRelationHandler().removeByObjectIds(objectIds);
        this.getRelationHandler().removeByParentObjectIds(objectIds);
        this.assign(objectIds).andThen(this::assignObject).invoke().andThen(this::removeBody).invoke();
    }

    default <D extends BodyData> Assign<ObjectDetail<O, R, D>> removeBody(Assign<ObjectDetail<O, R, D>> detailAssign) {
        return detailAssign
                .name("移除body")
                .addBranches(k -> Objects.requireNonNull(k.getObject()).getType(), (k, es) -> {
                    BodyProcessor<O, BodyDefiner, R, D, ObjectTypeDefiner<D>> processor = ObjectDispatcher.getProcessorByType(k);
                    return processor.removeBody(es);
                });
    }

}
