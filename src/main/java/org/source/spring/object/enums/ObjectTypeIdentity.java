package org.source.spring.object.enums;

import org.source.spring.object.AbstractValue;
import org.source.spring.object.entity.ObjectBodyEntityIdentity;
import org.source.spring.object.entity.ObjectEntityIdentity;
import org.source.spring.object.entity.RelationEntityIdentity;
import org.source.spring.object.processor.AbstractObjectProcessor;

public interface ObjectTypeIdentity {

    /**
     * 类型
     */
    Integer getType();

    /**
     * 描述
     */
    String getDesc();

    /**
     * object value class
     */
    <V extends AbstractValue> Class<V> getValueClass();

    /**
     * object processor
     */
    <O extends ObjectEntityIdentity, R extends RelationEntityIdentity, B extends ObjectBodyEntityIdentity, V extends AbstractValue,
            T extends ObjectTypeIdentity, K>
    Class<? extends AbstractObjectProcessor<O, R, B, V, T, K>> getObjectProcessor();

}