package org.source.spring.object.processor;

import lombok.Getter;
import org.source.spring.object.ObjectValueElement;
import org.source.spring.object.entity.ObjectEntity;
import org.source.spring.object.entity.RelationEntity;
import org.source.utility.tree.DefaultNode;
import org.source.utility.tree.Tree;

@Getter
public abstract class AbstractObjectProcessor<O extends ObjectEntity, R extends RelationEntity, E extends ObjectValueElement>
        implements ObjectProcessor<O, R, E, DefaultNode<String, E>> {
    private final Tree<String, E, DefaultNode<String, E>> docTree = DefaultNode.buildTree();
}
