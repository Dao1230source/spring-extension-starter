package org.source.spring.doc.processor;

import org.source.spring.doc.data.DocData;
import org.source.spring.doc.data.RequestDocData;
import org.source.spring.doc.data.VariableDocData;
import org.source.spring.object.ObjectData;
import org.source.spring.object.ObjectStatusEnum;
import org.source.spring.object.ValueData;
import org.source.spring.uid.Ids;
import org.source.utility.assign.Assign;
import org.source.utility.tree.DefaultNode;
import org.source.utility.tree.Tree;
import org.source.utility.tree.identity.AbstractNode;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public interface DocProcessor {
    Tree<String, DocData, DefaultNode<String, DocData>> getDocTree();

    default BiConsumer<DefaultNode<String, DocData>, DefaultNode<String, DocData>> getUpdateOldHandler() {
        return (n, old) -> {
            // 保留原objectId
            if (ObjectStatusEnum.CREATED.equals(n.getElement().getObjectStatus())) {
                n.getElement().setObjectId(old.getElement().getObjectId());
            }
            old.setElement(n.getElement());
            // 如果需要强制更新设置newObject=true
            old.getElement().setObjectStatus(ObjectStatusEnum.CACHED);
        };
    }

    /**
     * 根据Element.getId()的ids查询数据
     *
     * @param ids ids
     * @return {@literal Collection<ObjectData>}
     */
    default @NonNull Collection<ObjectData> findByIdsFromDb(Collection<String> ids) {
        return List.of();
    }

    /**
     * 插入数据到数据库
     *
     * @param nodeList nodeList
     */
    default void save2db(List<DefaultNode<String, ObjectData>> nodeList) {

    }

    default <E extends DocData> void sync2tree(Collection<E> es) {
        Set<String> ids = es.stream().map(DocData::getId).collect(Collectors.toSet());
        Set<String> treeExistsIds = getDocTree().find(n -> ids.contains(n.getId()) && Objects.nonNull(n.getElement().getObjectId()))
                .stream().map(AbstractNode::getId).collect(Collectors.toSet());
        Set<String> notExistsIds = ids.stream().filter(i -> !treeExistsIds.contains(i)).collect(Collectors.toSet());
        if (!CollectionUtils.isEmpty(notExistsIds)) {
            // 从数据中查询数据并添加到tree中
            List<DocData> dataFromDbList = this.findByIdsFromDb(notExistsIds).stream().map(k -> {
                ValueData valueData = k.getValue();
                valueData.setObjectStatus(ObjectStatusEnum.DATABASE);
                if (valueData instanceof DocData docData) {
                    docData.setId(k.getKey());
                    return docData;
                }
                return null;
            }).filter(Objects::nonNull).toList();
            getDocTree().add(dataFromDbList, this.getUpdateOldHandler());
        }
        // 如果相同id已存在，更新旧数据
        getDocTree().add(es, this.getUpdateOldHandler());
    }

    /**
     * 特殊处理
     * <pre>
     *     1、VariableDocData如果是自定义类，需要建立从属关系
     *     2、如果有接口文档 RequestDocData 数据，需要补全view数据
     * </pre>
     */
    default void specificHandle() {
        List<DefaultNode<String, DocData>> notBaseTypeVariableList = this.getDocTree().find(n ->
                n.getElement() instanceof VariableDocData variableDocData && variableDocData.notBaseType());
        Assign.build(notBaseTypeVariableList)
                .<String, DefaultNode<String, DocData>>addAcquire(ks -> this.getDocTree().find(n -> ks.contains(n.getElement().getKey())),
                        k -> k.getElement().getKey())
                .addAction(n -> ((VariableDocData) n.getElement()).getTypeName())
                .addAssemble(AbstractNode::addChild)
                .backAcquire().backAssign().invoke();
        this.getDocTree().find(n -> n.getElement() instanceof RequestDocData).forEach(n -> {
            if (n.getElement() instanceof RequestDocData requestDocData) {
                String methodId = requestDocData.getMethodId();
                DefaultNode<String, DocData> methodNode = this.getDocTree().getIdMap().get(methodId);
                Optional.ofNullable(methodNode).ifPresent(requestDocData::setRequestData);
            }
        });
    }

    default <E extends DocData> void save(Collection<E> es) {
        es.forEach(k -> k.setObjectStatus(ObjectStatusEnum.CREATED));
        this.sync2tree(es);
        this.specificHandle();
        Tree<String, ObjectData, DefaultNode<String, ObjectData>> tree = this.getDocTree()
                .cast(this::convert2Object, ObjectData::setParentObjectId);
        List<DefaultNode<String, ObjectData>> list = tree.getIdMap().values().stream()
                .filter(k -> !ObjectStatusEnum.DATABASE.equals(k.getElement().obtainObjectStatus())).toList();
        this.save2db(list);
        es.forEach(k -> k.setObjectStatus(ObjectStatusEnum.DATABASE));
    }

    default <E extends DocData> ObjectData convert2Object(E docData) {
        ObjectData objectData = new ObjectData();
        objectData.setObjectId(docData.getObjectId());
        objectData.setKey(docData.getId());
        objectData.setType(getType(docData));
        objectData.setValue(docData);
        // 如果objectId为null，表明是新增数据
        if (Objects.isNull(docData.getObjectId())) {
            objectData.setObjectId(Ids.stringId());
            docData.setObjectId(objectData.getObjectId());
        }
        return objectData;
    }

    /**
     * 类型
     */
    default Integer getType(ValueData valueData) {
        return -1;
    }

}
