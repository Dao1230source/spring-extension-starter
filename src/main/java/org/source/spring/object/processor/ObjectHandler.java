package org.source.spring.object.processor;

import org.source.spring.object.AbstractValue;
import org.source.spring.object.data.ObjectData;
import org.source.spring.object.entity.ObjectEntity;
import org.source.spring.object.entity.RelationEntity;

public interface ObjectHandler {

    /**
     * 类型
     */
    Integer getType();

    Class<? extends AbstractValue> getValueClass();

    Class<? extends ObjectProcessor<? extends ObjectEntity, ? extends RelationEntity, ? extends AbstractValue>> getObjectProcessor();

    <O extends ObjectEntity, V extends AbstractValue> V toObjectValue(O objectEntity);

    default <O extends ObjectEntity, V extends AbstractValue> ObjectData<V> toObjectData(O objectEntity) {
        ObjectData<V> objectData = new ObjectData<>();
        objectData.setObjectId(objectEntity.getObjectId());
        objectData.setType(objectEntity.getType());
        objectData.setKey(objectEntity.getKey());
        objectData.setValue(toObjectValue(objectEntity));
        return objectData;
    }
}
