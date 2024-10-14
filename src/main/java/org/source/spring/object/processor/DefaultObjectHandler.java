package org.source.spring.object.processor;

import org.source.spring.object.ObjectValueElement;
import org.source.spring.object.entity.ObjectEntity;
import org.source.spring.object.entity.RelationEntity;
import org.source.utility.tree.identity.AbstractNode;

public class DefaultObjectHandler implements ObjectHandler {
    @Override
    public Integer getType() {
        return -1;
    }

    @Override
    public Class<? extends ObjectValueElement> getValueClass() {
        return ObjectValueElement.class;
    }

    @Override
    public Class<? extends ObjectProcessor<? extends ObjectEntity, ? extends RelationEntity,
            ? extends ObjectValueElement, ? extends AbstractNode<String, ?, ?>>> getObjectProcessor() {
        return DefaultObjectProcessor.class;
    }

    @Override
    public <O extends ObjectEntity, V extends ObjectValueElement> V toObjectValue(O objectEntity) {
        return null;
    }
}
