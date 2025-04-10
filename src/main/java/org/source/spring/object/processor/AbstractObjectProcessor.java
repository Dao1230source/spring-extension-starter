package org.source.spring.object.processor;

import lombok.Getter;
import org.source.spring.object.AbstractValue;
import org.source.spring.object.entity.ObjectEntity;
import org.source.spring.object.entity.RelationEntity;
import org.source.spring.object.tree.ObjectNode;
import org.source.utility.tree.Tree;

@Getter
public abstract class AbstractObjectProcessor<O extends ObjectEntity, R extends RelationEntity, E extends AbstractValue>
        implements ObjectProcessor<O, R, E> {
    private final Tree<String, E, ObjectNode<String, E>> docTree = ObjectNode.buildTree();
}
