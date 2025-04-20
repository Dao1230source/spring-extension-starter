package org.source.spring.object.processor;

import org.source.spring.object.AbstractValue;
import org.source.spring.object.entity.ObjectEntity;
import org.source.spring.object.entity.RelationEntity;

public class DefaultObjectIdentity implements ObjectIdentity {
    @Override
    public Integer getType() {
        return -1;
    }

    @Override
    public Class<? extends AbstractValue> getValueClass() {
        return AbstractValue.class;
    }

    @Override
    public Class<? extends ObjectProcessor<? extends ObjectEntity, ? extends RelationEntity, ? extends AbstractValue>> getObjectProcessor() {
        return DefaultObjectProcessor.class;
    }

    @Override
    public <O extends ObjectEntity, V extends AbstractValue> V toObjectValue(O objectEntity) {
        return null;
    }
}
