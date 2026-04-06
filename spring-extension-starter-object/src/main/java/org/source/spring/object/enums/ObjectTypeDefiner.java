package org.source.spring.object.enums;

import org.source.spring.object.AbstractObjectProcessor;
import org.source.spring.object.entity.ObjectBodyEntityDefiner;
import org.source.spring.object.entity.ObjectEntityDefiner;
import org.source.spring.object.entity.RelationEntityDefiner;
import org.source.spring.object.handler.ObjectBodyValueHandlerDefiner;

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
    <V extends ObjectBodyValueHandlerDefiner> Class<V> getValueClass();

    /**
     * object processor
     */
    <O extends ObjectEntityDefiner, R extends RelationEntityDefiner, B extends ObjectBodyEntityDefiner,
            V extends ObjectBodyValueHandlerDefiner, T extends ObjectTypeDefiner, K>
    Class<? extends AbstractObjectProcessor<O, R, B, V, T, K>> getObjectProcessor();

}