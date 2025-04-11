package org.source.spring.doc.processor;

import lombok.Getter;
import org.source.spring.doc.DocDataContainer;
import org.source.spring.doc.data.*;
import org.source.spring.object.StatusEnum;
import org.source.spring.object.Value;
import org.source.spring.object.data.ObjectData;
import org.source.spring.object.entity.ObjectEntity;
import org.source.spring.object.entity.RelationEntity;
import org.source.spring.object.processor.AbstractObjectProcessor;
import org.source.spring.object.processor.RelationHandler;
import org.source.spring.object.tree.ObjectNode;
import org.source.utility.assign.Assign;
import org.source.utility.tree.Tree;
import org.source.utility.tree.identity.AbstractNode;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Getter
public abstract class AbstractDocProcessor<O extends ObjectEntity, R extends RelationEntity>
        extends AbstractObjectProcessor<O, R, DocData> {

    private final Tree<String, DocData, ObjectNode<String, DocData>> docTree = ObjectNode.buildTree();
    private final DocDataContainer docDataContainer = new DocDataContainer();

    public void process(RelationHandler relationHandler) {
        this.specificHandle();
        Tree<String, ObjectData<DocData>, ObjectNode<String, ObjectData<DocData>>> objectTree = this.getDocTree()
                .cast(this::convert2Object2, ObjectData::setParentObjectId,
                        (o, n) -> o.setStatus(n.getStatus()));
        List<ObjectData<DocData>> list = objectTree.getIdMap().values().stream()
                .filter(k -> !StatusEnum.DATABASE.equals(k.getStatus()))
                .map(AbstractNode::getElement).toList();
        this.saveObjectData(list, relationHandler);
        this.docTree.forEach((i, n) -> n.setStatus(StatusEnum.DATABASE));
    }

    /**
     * 特殊处理
     * <pre>
     *     1、VariableDocData如果是自定义类，需要建立从属关系
     *     2、如果有接口文档 RequestDocData 数据，需要补全view数据
     * </pre>
     */
    public void specificHandle() {
        // 变量不是基础类型的
        List<ObjectNode<String, DocData>> notBaseTypeVariableList = docTree.find(n ->
                n.getElement() instanceof VariableDocData variableDocData && variableDocData.notBaseType());
        Assign.build(notBaseTypeVariableList)
                .<String, ObjectNode<String, DocData>>addAcquire(ks -> docTree.find(n -> ks.contains(n.getElement().getName())),
                        k -> k.getElement().getName())
                .addAction(n -> ((VariableDocData) n.getElement()).getTypeName())
                .addAssemble(AbstractNode::addChild)
                .backAcquire().backAssign().invoke();
        // 接口文档对象
        docTree.find(n -> n.getElement() instanceof RequestDocData).forEach(n -> {
            if (n.getElement() instanceof RequestDocData requestDocData) {
                String methodId = requestDocData.getMethodId();
                ObjectNode<String, DocData> methodNode = this.getDocTree().getIdMap().get(methodId);
                Optional.ofNullable(methodNode).ifPresent(requestDocData::setRequestData);
            }
        });
        // 与父类或接口的数据合并
        docTree.find(ClassDocData::instanceOf).forEach(n -> {
            ClassDocData classDocData = (ClassDocData) n.getElement();
            List<String> superClassNames = classDocData.obtainSuperClassNames();
            if (CollectionUtils.isEmpty(superClassNames)) {
                return;
            }
            Optional<ObjectNode<String, DocData>> superClsNodeOptional = superClassNames.stream()
                    .map(docTree::getById).filter(Objects::nonNull).findFirst();
            if (superClsNodeOptional.isEmpty()) {
                return;
            }
            ObjectNode<String, DocData> superNode = superClsNodeOptional.get();
            DocData docData = superNode.getElement();
            if (docData instanceof ClassDocData superClsDocData) {
                classDocData.merge(superClsDocData);
                List<DocData> methodOrVariableList = n.getChildren().stream()
                        .filter(c -> MethodDocData.instanceOf(c) || VariableDocData.instanceOf(c))
                        .map(AbstractNode::getElement).toList();
                methodOrVariableList.forEach(c -> superNode.getChildren().stream()
                        .filter(sc -> c.getName().equals(sc.getElement().getName()))
                        .findFirst().ifPresent(sc -> c.merge(sc.getElement())));
            }
        });
    }

    /**
     * AbstractDoclet 中使用，转为tree结构，保存在内存中
     *
     * @param es es
     */
    public void add2Tree2(Collection<DocData> es) {
        this.add2Tree(es);
        this.getDocDataContainer().add(es);
    }


    public ObjectData<DocData> convert2Object2(DocData docData) {
        ObjectData<DocData> objectData = this.convert2Object(docData);
        objectData.setKey(docData.getId());
        return objectData;
    }

    @Override
    public Collection<DocData> maybeFromDb(Collection<DocData> es) {
        // DocData.key 为
        Set<String> keys = es.stream().map(DocData::getId).collect(Collectors.toSet());
        return getDocTree().find(n -> {
            if (keys.contains(n.getElement().getName())) {
                return !Objects.nonNull(n.getElement().getObjectId());
            }
            return true;
        }).stream().map(AbstractNode::getElement).collect(Collectors.toSet());
    }

    @Override
    public DocData valueFromObject(ObjectData<DocData> objectData) {
        Value value = objectData.getValue();
        if (value instanceof DocData docData) {
            docData.setId(objectData.getKey());
            return docData;
        }
        return null;
    }
}
