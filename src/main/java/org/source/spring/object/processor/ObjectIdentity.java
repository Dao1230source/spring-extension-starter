package org.source.spring.object.processor;

import org.source.spring.object.AbstractValue;
import org.source.spring.object.entity.ObjectBodyEntityIdentity;
import org.source.spring.object.entity.ObjectEntityIdentity;
import org.source.spring.object.entity.RelationEntityIdentity;

public interface ObjectIdentity<O extends ObjectEntityIdentity, R extends RelationEntityIdentity,
        B extends ObjectBodyEntityIdentity, V extends AbstractValue> {

    /**
     * 类型
     */
    Integer getType();

    /**
     * object value class
     */
    Class<V> getValueClass();

    /**
     * object processor
     */
    Class<? extends AbstractObjectProcessor<O, R, B, V>> getObjectProcessor();
}
