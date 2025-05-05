package org.source.spring.object.processor;

import org.source.spring.object.AbstractValue;
import org.source.spring.object.entity.ObjectBodyEntityIdentity;

public interface ObjectIdentity<B extends ObjectBodyEntityIdentity, V extends AbstractValue> {

    /**
     * 类型
     */
    Integer getType();

    Class<V> getValueClass();

    Class<? extends AbstractObjectBodyProcessor<B, V>> getObjectProcessor();
}
