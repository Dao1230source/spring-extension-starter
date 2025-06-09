package org.source.spring.doc.processor;

import lombok.Getter;
import org.source.spring.doc.DocDataContainer;
import org.source.spring.doc.data.*;
import org.source.spring.doc.enums.DocObjectTypeEnum;
import org.source.spring.exception.BizExceptionEnum;
import org.source.spring.object.AbstractValue;
import org.source.spring.object.StatusEnum;
import org.source.spring.object.Value;
import org.source.spring.object.data.ObjectFullData;
import org.source.spring.object.entity.ObjectBodyEntityIdentity;
import org.source.spring.object.entity.ObjectEntityIdentity;
import org.source.spring.object.entity.RelationEntityIdentity;
import org.source.spring.object.processor.AbstractObjectProcessor;
import org.source.spring.object.tree.ObjectNode;
import org.source.spring.utility.SpringUtil;
import org.source.utility.assign.Assign;
import org.source.utility.tree.Tree;
import org.source.utility.tree.identity.AbstractNode;
import org.source.utility.tree.identity.Node;
import org.source.utility.utils.Enums;
import org.source.utility.utils.Streams;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;

@Getter
public abstract class AbstractDocProcessor<O extends ObjectEntityIdentity, R extends RelationEntityIdentity, B extends ObjectBodyEntityIdentity>
        extends AbstractObjectProcessor<O, R, B, DocData> {

    private final DocDataContainer docDataContainer = new DocDataContainer();

    @Override
    public List<ObjectFullData<DocData>> obtainObjectData() {
        Tree<String, ObjectFullData<DocData>, ObjectNode<String, ObjectFullData<DocData>>> objectTree = this.getDocTree()
                .cast(this::convert2Object, ObjectFullData::setParentObjectId,
                        (o, n) -> o.setStatus(n.getStatus()));
        objectTree.forEach((k, v) -> {
            if (v.getElement().getValue() instanceof RequestDocData requestDocData && Objects.nonNull(requestDocData.getMethodNode())) {
                List<ObjectNode<String, DocData>> objectNodes = Node.recursiveChildren(requestDocData.getMethodNode());
                List<String> objectIds = Streams.map(objectNodes, n -> n.getElement().getObjectId()).filter(Objects::nonNull).toList();
                // 接口请求对应的node设置 belongId
                objectTree.find(n -> objectIds.contains(n.getId()))
                        .forEach(n -> n.getElement().setBelongId(v.getId()));
            }
        });
        return objectTree.getIdMap().values().stream()
                .filter(k -> !StatusEnum.DATABASE.equals(k.getStatus()))
                .map(AbstractNode::getElement).toList();
    }

    /**
     * 特殊处理
     * <pre>
     *     1、VariableDocData如果是自定义类，需要建立从属关系
     *     2、如果有接口文档 RequestDocData 数据，需要补全view数据
     * </pre>
     */
    @Override
    public void afterTransfer() {
        // 变量不是基础类型的
        List<ObjectNode<String, DocData>> notBaseTypeVariableList = docTree.find(n ->
                n.getElement() instanceof VariableDocData variableDocData && variableDocData.notBaseType());
        Function<Collection<String>, Collection<ObjectNode<String, DocData>>> fetcher = ks -> docTree.find(n -> ks.contains(n.getElement().getName()));
        Assign.build(notBaseTypeVariableList)
                .addAcquire(fetcher, k -> k.getElement().getName())
                .addAction(n -> ((VariableDocData) n.getElement()).getTypeName())
                .addAssemble(AbstractNode::addChild)
                .backAcquire().backAssign().invoke();
        // 接口文档对象
        docTree.find(n -> n.getElement() instanceof RequestDocData).forEach(n -> {
            RequestDocData requestDocData = (RequestDocData) n.getElement();
            String methodId = requestDocData.getMethodId();
            requestDocData.setMethodNode(this.getDocTree().getIdMap().get(methodId));
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
                List<DocData> methodOrVariableList = Streams.of(n.getChildren())
                        .filter(c -> MethodDocData.instanceOf(c) || VariableDocData.instanceOf(c))
                        .map(AbstractNode::getElement).toList();
                methodOrVariableList.forEach(c -> Streams.of(superNode.getChildren())
                        .filter(sc -> c.getName().equals(sc.getElement().getName()))
                        .findFirst().ifPresent(sc -> c.merge(sc.getElement())));
            }
        });
    }

    @Override
    public ObjectFullData<DocData> convert2Object(DocData docData) {
        ObjectFullData<DocData> objectFullData = super.convert2Object(docData);
        objectFullData.setKey(docData.getId());
        objectFullData.setRelationType(docData.getRelationType());
        return objectFullData;
    }

    @Override
    public DocData valueFromObject(ObjectFullData<DocData> objectFullData) {
        Value value = objectFullData.getValue();
        if (value instanceof DocData docData) {
            docData.setId(objectFullData.getKey());
            return docData;
        }
        return null;
    }

    @Override
    public Map<Integer, ? extends AbstractObjectProcessor<? extends ObjectEntityIdentity, ? extends RelationEntityIdentity,
            ? extends ObjectBodyEntityIdentity, ? extends AbstractValue>> allObjectProcessors() {
        return Enums.toMap(DocObjectTypeEnum.class, DocObjectTypeEnum::getType,
                e -> (AbstractObjectProcessor<? extends ObjectEntityIdentity, ? extends RelationEntityIdentity, ? extends ObjectBodyEntityIdentity, ? extends AbstractValue>) SpringUtil.getBean(e.getObjectProcessor()));
    }

    @Override
    public Integer getObjectType(DocData docData) {
        DocObjectTypeEnum anEnum = DocObjectTypeEnum.getByValueClass(docData.getClass());
        if (Objects.isNull(anEnum)) {
            throw BizExceptionEnum.DOC_DATA_CLASS_NOT_DEFINED.except("class:{}", docData.getClass());
        }
        return anEnum.getType();
    }

    @Override
    public DocData toObjectValue(Integer type, ObjectBodyEntityIdentity objectBodyEntity) {
        return DocObjectTypeEnum.toObjectValue(type, objectBodyEntity);
    }
}