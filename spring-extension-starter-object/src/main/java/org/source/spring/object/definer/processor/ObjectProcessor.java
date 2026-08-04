package org.source.spring.object.definer.processor;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.collections4.CollectionUtils;
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
import org.source.spring.object.domain.RelationOperate;
import org.source.spring.object.domain.RelationUniqueKey;
import org.source.spring.object.enums.RelateOperateEnum;
import org.source.spring.object.enums.RelationTypeEnum;
import org.source.utility.assign.Assign;
import org.source.utility.assign.InterruptStrategyEnum;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.tree.EnhanceTree;
import org.source.utility.utils.Streams;

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

    @EqualsAndHashCode(callSuper = true)
    @Data
    class RelationTemp<O, R> extends RelationOperate {
        private O object;
        private O parentObject;
        private R relation;

        public void valid() {
            BaseExceptionEnum.NOT_EMPTY.notEmpty(this.getObjectId(), "关联操作时objectId必须不为空");
            BaseExceptionEnum.NOT_EMPTY.notEmpty(this.getParentObjectId(), "关联操作时parentObjectId必须不为空");
            BaseExceptionEnum.NOT_NULL.nonNull(this.getRelateOperate(), "关联操作时relateOperate必须不为空");
        }

        public static <O, R> RelationTemp<O, R> of(RelationOperate relate) {
            RelationTemp<O, R> temp = new RelationTemp<>();
            temp.setObjectId(relate.getObjectId());
            temp.setParentObjectId(relate.getParentObjectId());
            temp.setSorted(relate.getSorted());
            temp.setRelateOperate(relate.getRelateOperate());
            temp.valid();
            return temp;
        }
    }

    default void saveRelation(Collection<RelationOperate> relates) {
        if (CollectionUtils.isEmpty(relates)) {
            return;
        }
        List<RelationTemp<O, R>> relateTempList = Assign.build(relates)
                .<RelationTemp<O, R>>cast(RelationTemp::of)
                .name("查询objectId和parentObjectId对应的Object记录")
                .addAcquire(this.getObjectHandler()::find, O::getObjectId)
                .addAction(RelationTemp::getObjectId)
                .addAssemble(RelationTemp::setObject)
                .backAcquire()
                .addAction(RelationTemp::getParentObjectId)
                .addAssemble(RelationTemp::setParentObject)
                .backAcquire().backAssign()
                .addBranch(k -> Objects.nonNull(k.getObject()) && Objects.nonNull(k.getParentObject()))
                .name("查询需要删除的Relation记录")
                .addAcquire(this.getRelationHandler()::findByRelateUniqueKeys, RelationUniqueKey::of)
                .addAction(RelationUniqueKey::of)
                .addAssemble(RelationTemp::setRelation)
                .backAcquire()
                .afterProcessor((e, kt) -> {
                    if (Objects.nonNull(e.getRelation())) {
                        BaseExceptionEnum.CIRCULAR_REFERENCE_EXCEPTION.isTrue(Objects.equals(e.getRelation().getObjectId() + e.getRelation().getParentObjectId(), e.getParentObjectId() + e.getObjectId()),
                                "已有关联关系objectId:{},parentObjectId:{}，不可循环关联", e.getRelation().getObjectId(), e.getRelation().getParentObjectId());
                    }
                })
                .backAssign().backUppermost()
                .invoke().toList();
        List<R> toAddList = Streams.retain(relateTempList, k -> RelateOperateEnum.ADD.name().equals(k.getRelateOperate())
                        && Objects.nonNull(k.getObject()) && Objects.nonNull(k.getParentObject()))
                .map(this::relateDataToRelation).toList();
        if (CollectionUtils.isNotEmpty(toAddList)) {
            this.getRelationHandler().saveRelations(toAddList);
        }
        List<Long> toDeleteList = Streams.retain(relateTempList, k -> Objects.nonNull(k.getRelation()))
                .map(RelationTemp::getRelation)
                .map(R::getId).toList();
        if (CollectionUtils.isNotEmpty(toDeleteList)) {
            this.getRelationHandler().removeByRelationIds(toDeleteList);
        }
    }

    default R relateDataToRelation(RelationOperate operate) {
        R relation = this.getRelationHandler().newRelation();
        relation.setCreateUser(this.getUserId());
        relation.setCreateTime(LocalDateTime.now());
        if (Objects.isNull(operate.getType())) {
            relation.setType(RelationTypeEnum.LINKS.getType());
        }
        relation.setParentObjectId(operate.getParentObjectId());
        relation.setObjectId(operate.getObjectId());
        relation.setSorted(operate.getSorted());
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
