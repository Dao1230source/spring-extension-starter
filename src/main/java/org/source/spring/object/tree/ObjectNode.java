package org.source.spring.object.tree;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.source.spring.object.StatusEnum;
import org.source.utility.tree.Tree;
import org.source.utility.tree.identity.AbstractNode;
import org.source.utility.tree.identity.Element;

@EqualsAndHashCode(callSuper = true, exclude = {"oldElement", "oldStatus"})
@Data
public class ObjectNode<I, E extends Element<I>> extends AbstractNode<I, E, ObjectNode<I, E>> {
    private StatusEnum status;
    @JsonIgnore
    private transient E oldElement;
    @JsonIgnore
    private transient StatusEnum oldStatus;
    /**
     * 与parentNode的关联关系
     */
    private Integer relationType;

    @SuppressWarnings("unchecked")
    @Override
    public <J, F extends Element<J>, O extends AbstractNode<J, F, O>> O emptyNode() {
        return (O) new ObjectNode<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <J, F extends Element<J>, O extends AbstractNode<J, F, O>> Tree<J, F, O> emptyTree() {
        return (Tree<J, F, O>) ObjectNode.buildTree();
    }

    public static <I, E extends Element<I>> Tree<I, E, ObjectNode<I, E>> buildTree() {
        return new Tree<>(ObjectNode::new, node -> {
        });
    }
}