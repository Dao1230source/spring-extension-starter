package org.source.spring.object.processor;

import org.source.spring.object.ObjectValueElement;
import org.source.spring.object.data.ObjectData;
import org.source.spring.object.entity.ObjectEntity;
import org.source.spring.object.entity.RelationEntity;
import org.source.utility.tree.identity.AbstractNode;

public interface ObjectHandler {

    /**
     * 类型
     */
    Integer getType();

    Class<? extends ObjectValueElement> getValueClass();

    Class<? extends ObjectProcessor<? extends ObjectEntity, ? extends RelationEntity,
            ? extends ObjectValueElement, ? extends AbstractNode<String, ?, ?>>> getObjectProcessor();

    <O extends ObjectEntity, V extends ObjectValueElement> V toObjectValue(O objectEntity);

    default <O extends ObjectEntity, V extends ObjectValueElement> ObjectData<V> toObjectData(O objectEntity) {
        ObjectData<V> objectData = new ObjectData<>();
        objectData.setObjectId(objectData.getObjectId());
        objectData.setType(objectData.getType());
        objectData.setKey(objectData.getKey());
        objectData.setValue(toObjectValue(objectEntity));
        return objectData;
    }
}
