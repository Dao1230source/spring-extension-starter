package org.source.spring.object.enums;

import org.source.spring.object.AbstractValue;
import org.source.spring.object.entity.ObjectBodyEntityDefiner;
import org.source.spring.object.entity.ObjectEntityDefiner;
import org.source.spring.object.entity.RelationEntityDefiner;
import org.source.spring.object.AbstractObjectProcessor;

public interface ObjectTypeDefiner {

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
    <O extends ObjectEntityDefiner, R extends RelationEntityDefiner, B extends ObjectBodyEntityDefiner,
            V extends AbstractValue, T extends ObjectTypeDefiner, K>
    Class<? extends AbstractObjectProcessor<O, R, B, V, T, K>> getObjectProcessor();

}