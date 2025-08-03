package org.source.spring.doc.object;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.source.spring.doc.DocDataContainer;
import org.source.spring.doc.data.*;
import org.source.spring.doc.object.entity.DocEntityDefiner;
import org.source.spring.doc.object.enums.DocObjectTypeEnum;
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
    public Function<ObjectElement<DocData>, String> getFullDataIdGetter() {
        return o -> o.getValue().getFullName();
    }

    @Override
    public Function<ObjectElement<DocData>, String> getFullDataParentIdGetter() {
        return o -> o.getValue().getParentName();
    }


    @Override
    public EnhanceTree<String, ObjectElement<DocData>, ObjectNode<String, ObjectElement<DocData>>> handleDbDataTree() {
        return docCustomTree(super.handleDbDataTree());
    }

    @Override
    public EnhanceTree<String, ObjectElement<DocData>, ObjectNode<String, ObjectElement<DocData>>> handleValueDataTree() {
        return docCustomTree(super.handleValueDataTree());
    }

    @NotNull
    private EnhanceTree<String, ObjectElement<DocData>, ObjectNode<String, ObjectElement<DocData>>> docCustomTree(
            EnhanceTree<String, ObjectElement<DocData>, ObjectNode<String, ObjectElement<DocData>>> tree) {
        tree.setIdGetter(n -> Node.getProperty(n, this.getFullDataIdGetter()));
        tree.setParentIdGetter(n -> Node.getProperty(n, this.getFullDataParentIdGetter()));
        tree.setAfterAddHandler((n, parent) ->
                Node.setProperty(n, ObjectElement::setParentObjectId, Node.getProperty(parent, ObjectElement::getObjectId)));
        return tree;
    }

    @Override
    public boolean nodeEquals(ObjectNode<String, ObjectElement<DocData>> n, ObjectNode<String, ObjectElement<DocData>> old) {
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
            if (v.getElement().getValue() instanceof RequestDocData requestDocData && Objects.nonNull(requestDocData.getMethodNode())) {
                List<ObjectNode<String, ObjectElement<DocData>>> objectNodes = Node.recursiveChildren(requestDocData.getMethodNode(), true);
                List<String> objectIds = Streams.map(objectNodes, n -> n.getElement().getObjectId())
                        .filter(Objects::nonNull).toList();
                // 接口请求对应的node设置 belongId
                this.getObjectTree().find(n -> objectIds.contains(n.getId())).forEach(n -> {
                    n.getElement().setBelongId(v.getId());
                    if (StatusEnum.CACHED.equals(n.getStatus())) {
                        n.setStatus(StatusEnum.CACHED_RELATION);
                        if (Objects.nonNull(n.getElement())) {
                            n.getElement().setStatus(n.getStatus());
                        }
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
        Tree<String, ObjectElement<DocData>, ObjectNode<String, ObjectElement<DocData>>> objectTree = this.getObjectTree();
        // 变量不是基础类型的
        List<ObjectNode<String, ObjectElement<DocData>>> notBaseTypeVariableList = this.getObjectTree().find(n ->
                n.getElement().getValue() instanceof VariableDocData variableDocData && variableDocData.notBaseType());
        Function<Collection<String>, Collection<ObjectNode<String, ObjectElement<DocData>>>> fetcher =
                ks -> objectTree.find(n -> ks.contains(n.getElement().getName()));
        Assign.build(notBaseTypeVariableList)
                .addAcquire(fetcher, k -> k.getElement().getName())
                .addAction(n -> ((VariableDocData) n.getElement().getValue()).getTypeName())
                .addAssemble(AbstractNode::addChild)
                .backAcquire().backAssign().invoke();
        objectTree.forEach((k, n) -> {
            // 接口文档对象
            if (n.getElement().getValue() instanceof RequestDocData requestDocData) {
                String methodId = requestDocData.getMethodId();
                requestDocData.setMethodNode(objectTree.getIdMap().get(methodId));
            }
            this.mergeSuperClassData(n);
        });
    }

    private void mergeSuperClassData(ObjectNode<String, ObjectElement<DocData>> n) {
        // 与父类或接口的数据合并
        if (n.getElement().getValue() instanceof ClassDocData classDocData) {
            List<String> superClassNames = classDocData.obtainSuperClassNames();
            if (CollectionUtils.isEmpty(superClassNames)) {
                return;
            }
            Optional<ObjectNode<String, ObjectElement<DocData>>> superClsNodeOptional = superClassNames.stream()
                    .map(this.getObjectTree()::getById).filter(Objects::nonNull).findFirst();
            if (superClsNodeOptional.isEmpty()) {
                return;
            }
            ObjectNode<String, ObjectElement<DocData>> superNode = superClsNodeOptional.get();
            DocData docData = superNode.getElement().getValue();
            if (!(docData instanceof ClassDocData superClsDocData)) {
                return;
            }
            classDocData.merge(superClsDocData);
            Streams.of(n.getChildren()).forEach(c -> {
                if (n.getElement().getValue() instanceof MethodDocData || c.getElement().getValue() instanceof VariableDocData) {
                    Streams.of(superNode.getChildren()).filter(sc -> c.getElement().getName().equals(sc.getElement().getName()))
                            .findFirst().ifPresent(sc -> this.mergeValue(c.getOldElement(), sc.getOldElement()));
                }
            });
        }
    }

    @Override
    public ObjectElement<DocData> convert2Object(DocData docData) {
        ObjectElement<DocData> objectElement = super.convert2Object(docData);
        objectElement.setName(docData.getName());
        objectElement.setRelationType(docData.getRelationType());
        return objectElement;
    }

    @Override
    public B data2ObjectBodyEntity(ObjectElement<DocData> data) {
        B b = super.data2ObjectBodyEntity(data);
        b.setParentName(Objects.requireNonNullElse(data.getValue().getParentName(), PARENT_NAME_DEFAULT));
        return b;
    }

    @Override
    public void handlerAfterObjectBodyConvertToFullData(ObjectElement<DocData> fullData, Map<K, B> objectMap) {
        if (fullData.getValue().getClass().isAssignableFrom(DocData.class)) {
            fullData.setRelationType(fullData.getValue().getRelationType());
        }
    }
}