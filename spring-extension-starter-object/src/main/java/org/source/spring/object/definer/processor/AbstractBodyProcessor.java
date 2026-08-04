package org.source.spring.object.definer.processor;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.source.spring.object.BodyData;
import org.source.spring.object.ObjectElement;
import org.source.spring.object.ObjectNode;
import org.source.spring.object.definer.entity.BodyDefiner;
import org.source.spring.object.definer.entity.ObjectDefiner;
import org.source.spring.object.definer.entity.RelationDefiner;
import org.source.spring.object.definer.enums.ObjectTypeDefiner;
import org.source.utility.tree.EnhanceTree;
import org.source.utility.tree.define.IdDefiner;
import org.source.utility.tree.define.Node;

import java.util.function.Function;

public abstract class AbstractBodyProcessor<
        O extends ObjectDefiner,
        B extends BodyDefiner,
        R extends RelationDefiner,
        D extends BodyData,
        T extends ObjectTypeDefiner<D>> implements BodyProcessor<O, B, R, D, T> {
    @Getter
    private final EnhanceTree<String, ObjectElement<D>, ObjectNode<D>> objectTree = EnhanceTree.of(new ObjectNode<>());

    private final Function<ObjectNode<D>, @Nullable String> objectIdGetter = n -> Node.getProperty(n, e -> this.dataId(e.getData()));
    private final Function<ObjectNode<D>, @Nullable String> objectParentIdGetter = n -> Node.getProperty(n, e -> this.parentDataId(e.getData()));
    @Getter
    protected final IdDefiner<String, ObjectElement<D>, ObjectNode<D>> dataIdDefiner = new IdDefiner<>("data", objectIdGetter, objectParentIdGetter);

}
