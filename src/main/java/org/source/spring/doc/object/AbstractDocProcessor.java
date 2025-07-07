package org.source.spring.doc.object;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.source.spring.doc.DocDataContainer;
import org.source.spring.doc.data.*;
import org.source.spring.doc.object.entity.DocEntityDefiner;
import org.source.spring.doc.object.enums.DocObjectTypeEnum;
import org.source.spring.object.AbstractObjectProcessor;
import org.source.spring.object.StatusEnum;
import org.source.spring.object.data.ObjectFullData;
import org.source.spring.object.entity.ObjectEntityDefiner;
import org.source.spring.object.entity.RelationEntityDefiner;
import org.source.spring.object.handler.ObjectBodyDbHandlerDefiner;
import org.source.spring.object.handler.ObjectDbHandlerDefiner;
import org.source.spring.object.handler.ObjectTypeHandlerDefiner;
import org.source.spring.object.handler.RelationDbHandlerDefiner;
import org.source.spring.object.tree.ObjectNode;
import org.source.utility.assign.Assign;
import org.source.utility.tree.Tree;
import org.source.utility.tree.identity.AbstractNode;
import org.source.utility.tree.identity.Node;
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
    public Function<ObjectFullData<DocData>, String> getFullDataIdGetter() {
        return o -> o.getValue().getFullName();
    }

    @Override
    public Function<ObjectFullData<DocData>, String> getFullDataParentIdGetter() {
        return o -> o.getValue().getParentName();
    }


    @Override
    public Tree<String, ObjectFullData<DocData>, ObjectNode<String, ObjectFullData<DocData>>> handleDbDataTree() {
        return docCustomTree(super.handleDbDataTree());
    }

    @Override
    public Tree<String, ObjectFullData<DocData>, ObjectNode<String, ObjectFullData<DocData>>> handleValueDataTree() {
        return docCustomTree(super.handleValueDataTree());
    }

    @NotNull
    private Tree<String, ObjectFullData<DocData>, ObjectNode<String, ObjectFullData<DocData>>> docCustomTree(
            Tree<String, ObjectFullData<DocData>, ObjectNode<String, ObjectFullData<DocData>>> tree) {
        tree.setIdGetter(n -> Node.getProperty(n, this.getFullDataIdGetter()));
        tree.setParentIdGetter(n -> Node.getProperty(n, this.getFullDataParentIdGetter()));
        tree.setFinallyHandler((n, parent) ->
                Node.setProperty(n, ObjectFullData::setParentObjectId, Node.getProperty(parent, ObjectFullData::getObjectId)));
        return tree;
    }

    @Override
    public boolean nodeEquals(ObjectNode<String, ObjectFullData<DocData>> n, ObjectNode<String, ObjectFullData<DocData>> old) {
        ObjectFullData<DocData> eleNew = n.getElement();
        ObjectFullData<DocData> eleOld = old.getElement();
        return eleNew.getValue().equals(eleOld.getValue())
                && eleNew.getType().equals(eleOld.getType())
                && eleNew.getRelationType().equals(eleOld.getRelationType());
    }

    @Override
    public void beforePersist() {
        super.beforePersist();
        this.getObjectTree().forEach((k, v) -> {
            if (v.getElement().getValue() instanceof RequestDocData requestDocData && Objects.nonNull(requestDocData.getMethodNode())) {
                List<ObjectNode<String, ObjectFullData<DocData>>> objectNodes = Node.recursiveChildren(requestDocData.getMethodNode(), true);
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
        Tree<String, ObjectFullData<DocData>, ObjectNode<String, ObjectFullData<DocData>>> objectTree = this.getObjectTree();
        // 变量不是基础类型的
        List<ObjectNode<String, ObjectFullData<DocData>>> notBaseTypeVariableList = this.getObjectTree().find(n ->
                n.getElement().getValue() instanceof VariableDocData variableDocData && variableDocData.notBaseType());
        Function<Collection<String>, Collection<ObjectNode<String, ObjectFullData<DocData>>>> fetcher =
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

    private void mergeSuperClassData(ObjectNode<String, ObjectFullData<DocData>> n) {
        // 与父类或接口的数据合并
        if (n.getElement().getValue() instanceof ClassDocData classDocData) {
            List<String> superClassNames = classDocData.obtainSuperClassNames();
            if (CollectionUtils.isEmpty(superClassNames)) {
                return;
            }
            Optional<ObjectNode<String, ObjectFullData<DocData>>> superClsNodeOptional = superClassNames.stream()
                    .map(this.getObjectTree()::getById).filter(Objects::nonNull).findFirst();
            if (superClsNodeOptional.isEmpty()) {
                return;
            }
            ObjectNode<String, ObjectFullData<DocData>> superNode = superClsNodeOptional.get();
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
    public ObjectFullData<DocData> convert2Object(DocData docData) {
        ObjectFullData<DocData> objectFullData = super.convert2Object(docData);
        objectFullData.setName(docData.getName());
        objectFullData.setRelationType(docData.getRelationType());
        return objectFullData;
    }

    @Override
    public B data2ObjectBodyEntity(ObjectFullData<DocData> data) {
        B b = super.data2ObjectBodyEntity(data);
        b.setParentName(Objects.requireNonNullElse(data.getValue().getParentName(), PARENT_NAME_DEFAULT));
        return b;
    }

    @Override
    public void handlerAfterObjectBodyConvertToFullData(ObjectFullData<DocData> fullData, Map<K, B> objectMap) {
        if (fullData.getValue().getClass().isAssignableFrom(DocData.class)) {
            fullData.setRelationType(fullData.getValue().getRelationType());
        }
    }
}