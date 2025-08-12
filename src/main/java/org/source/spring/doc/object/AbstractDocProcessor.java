package org.source.spring.doc.object;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.source.spring.doc.DocDataContainer;
import org.source.spring.doc.data.*;
import org.source.spring.doc.object.entity.DocEntityDefiner;
import org.source.spring.doc.object.enums.DocObjectTypeEnum;
import org.source.spring.doc.object.enums.DocRelationTypeEnum;
import org.source.spring.object.AbstractObjectProcessor;
import org.source.spring.object.ObjectElement;
import org.source.spring.object.ObjectNode;
import org.source.spring.object.StatusEnum;
import org.source.spring.object.entity.ObjectEntityDefiner;
import org.source.spring.object.entity.RelationEntityDefiner;
import org.source.spring.object.handler.ObjectBodyDbHandlerDefiner;
import org.source.spring.object.handler.ObjectDbHandlerDefiner;
import org.source.spring.object.handler.ObjectTypeHandlerDefiner;
import org.source.spring.object.handler.RelationDbHandlerDefiner;
import org.source.utility.assign.Assign;
import org.source.utility.tree.EnhanceTree;
import org.source.utility.tree.Tree;
import org.source.utility.tree.define.AbstractNode;
import org.source.utility.tree.define.Node;
import org.source.utility.utils.Streams;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;

@Getter
public abstract class AbstractDocProcessor
        <O extends ObjectEntityDefiner, R extends RelationEntityDefiner, B extends DocEntityDefiner, K extends DocUniqueKey>
        extends AbstractObjectProcessor<O, R, B, DocData, DocObjectTypeEnum, K> {
    public static final String PARENT_NAME_DEFAULT = "root";
    private final DocDataContainer docDataContainer = new DocDataContainer();

    protected AbstractDocProcessor(ObjectDbHandlerDefiner<O> objectDbHandlerDefiner,
                                   ObjectBodyDbHandlerDefiner<B, DocData, K> objectBodyDbHandlerDefiner,
                                   RelationDbHandlerDefiner<R> relationDbHandlerDefiner,
                                   ObjectTypeHandlerDefiner<B, DocData, DocObjectTypeEnum> objectTypeHandler) {
        super(objectDbHandlerDefiner, objectBodyDbHandlerDefiner, relationDbHandlerDefiner, objectTypeHandler);
    }

    @Override
    public Function<DocData, String> getValueIdGetter() {
        return DocData::getFullName;
    }

    @Override
    public Function<B, String> getObjectBodyIdGetter() {
        return b -> DocData.obtainFullName(b.getName(), b.getParentName());
    }

    @Override
    public Function<ObjectElement<DocData>, String> getElementIdGetter() {
        return o -> o.getValue().getFullName();
    }

    @Override
    public Function<ObjectElement<DocData>, String> getElementParentIdGetter() {
        return o -> o.getValue().getParentName();
    }


    @Override
    public EnhanceTree<String, ObjectElement<DocData>, ObjectNode<DocData>> handleDbDataTree() {
        return docCustomTree(super.handleDbDataTree());
    }

    @Override
    public EnhanceTree<String, ObjectElement<DocData>, ObjectNode<DocData>> handleValueDataTree() {
        return docCustomTree(super.handleValueDataTree());
    }

    @NotNull
    private EnhanceTree<String, ObjectElement<DocData>, ObjectNode<DocData>> docCustomTree(
            EnhanceTree<String, ObjectElement<DocData>, ObjectNode<DocData>> tree) {
        tree.setIdGetter(n -> Node.getProperty(n, this.getElementIdGetter()));
        tree.setParentIdGetter(n -> Node.getProperty(n, this.getElementParentIdGetter()));
        tree.setAfterAddHandler((n, parent) ->
                Node.setProperty(n, ObjectElement::setParentObjectId, Node.getProperty(parent, ObjectElement::getObjectId)));
        return tree;
    }

    @Override
    public boolean nodeEquals(ObjectNode<DocData> n, ObjectNode<DocData> old) {
        ObjectElement<DocData> eleNew = n.getElement();
        ObjectElement<DocData> eleOld = old.getElement();
        return eleNew.getValue().equals(eleOld.getValue())
                && eleNew.getType().equals(eleOld.getType())
                && eleNew.getRelationType().equals(eleOld.getRelationType());
    }

    @Override
    public void beforePersist() {
        super.beforePersist();
        this.getObjectTree().forEach((k, v) -> {
            if (v.getElement().getValue() instanceof DocRequestData docRequestData && Objects.nonNull(docRequestData.getMethodNode())) {
                List<ObjectNode<DocData>> objectNodes = Node.recursiveChildren(docRequestData.getMethodNode(), true);
                List<String> objectIds = Streams.map(objectNodes, n -> n.getElement().getObjectId())
                        .filter(Objects::nonNull).toList();
                // 接口请求对应的node设置 belongId
                this.getObjectTree().find(n -> objectIds.contains(n.getId())).forEach(n -> {
                    if (StatusEnum.CACHED.equals(n.getStatus())) {
                        n.setStatus(StatusEnum.CACHED_RELATION);
                    }
                });
            }
        });
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
        Tree<String, ObjectElement<DocData>, ObjectNode<DocData>> objectTree = this.getObjectTree();
        // 变量不是基础类型的
        List<ObjectNode<DocData>> notBaseTypeVariableList = this.getObjectTree().find(n ->
                n.getElement().getValue() instanceof DocVariableData variableDocData && variableDocData.notBaseType());
        Function<Collection<String>, Collection<ObjectNode<DocData>>> fetcher =
                ks -> objectTree.find(n -> ks.contains(n.getElement().getName()));
        Assign.build(notBaseTypeVariableList)
                .addAcquire(fetcher, k -> k.getElement().getName())
                .addAction(n -> ((DocVariableData) n.getElement().getValue()).getTypeName())
                .addAssemble(AbstractNode::addChild)
                .backAcquire().backAssign().invoke();
        objectTree.forEach((k, n) -> {
            // 接口文档对象
            if (n.getElement().getValue() instanceof DocRequestData docRequestData) {
                String methodId = docRequestData.getMethodId();
                ObjectNode<DocData> methodNode = objectTree.getIdMap().get(methodId);
                Node.recursiveChildren(methodNode, true).forEach(on -> {
                    on.appendToParent(n);
                    if (CollectionUtils.isEmpty(n.getRelationTypes())) {
                        n.setRelationTypes(new ArrayList<>());
                        n.getRelationTypes().add(on.getElement().getRelationType());
                    }
                    n.getRelationTypes().add(DocRelationTypeEnum.REQUEST.getType());
                });
            }
            this.mergeSuperClassData(n);
        });
    }

    private void mergeSuperClassData(ObjectNode<DocData> n) {
        // 与父类或接口的数据合并
        if (n.getElement().getValue() instanceof DocClassData docClassData) {
            List<String> superClassNames = docClassData.obtainSuperClassNames();
            if (CollectionUtils.isEmpty(superClassNames)) {
                return;
            }
            Optional<ObjectNode<DocData>> superClsNodeOptional = superClassNames.stream()
                    .map(this.getObjectTree()::getById).filter(Objects::nonNull).findFirst();
            if (superClsNodeOptional.isEmpty()) {
                return;
            }
            ObjectNode<DocData> superNode = superClsNodeOptional.get();
            DocData docData = superNode.getElement().getValue();
            if (!(docData instanceof DocClassData superClsDocData)) {
                return;
            }
            docClassData.merge(superClsDocData);
            Streams.of(n.getChildren()).forEach(c -> {
                if (n.getElement().getValue() instanceof DocMethodData || c.getElement().getValue() instanceof DocVariableData) {
                    Streams.of(superNode.getChildren()).filter(sc -> c.getElement().getName().equals(sc.getElement().getName()))
                            .findFirst().ifPresent(sc -> this.mergeValue(c.getElement(), sc.getElement()));
                }
            });
        }
    }

    @Override
    public B data2ObjectBodyEntity(ObjectNode<DocData> data) {
        B b = super.data2ObjectBodyEntity(data);
        b.setParentName(Objects.requireNonNullElse(data.getElement().getValue().getParentName(), PARENT_NAME_DEFAULT));
        return b;
    }

    @Override
    public void convertToObjectElementAfterProcessor(ObjectElement<DocData> fullData, Map<String, O> objectMap) {
        if (fullData.getValue().getClass().isAssignableFrom(DocData.class)) {
            fullData.setRelationType(fullData.getValue().getRelationType());
        }
    }
}