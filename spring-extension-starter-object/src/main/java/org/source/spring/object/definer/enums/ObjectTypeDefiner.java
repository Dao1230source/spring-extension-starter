package org.source.spring.object.definer.enums;

import org.source.spring.object.ObjectBodyData;
import org.source.spring.object.definer.entity.ObjectBodyEntityDefiner;
import org.source.spring.object.definer.entity.ObjectEntityDefiner;
import org.source.spring.object.definer.entity.RelationEntityDefiner;
import org.source.spring.object.definer.processor.AbstractObjectProcessor;

public interface ObjectTypeDefiner<
        O extends ObjectEntityDefiner, R extends RelationEntityDefiner, B extends ObjectBodyEntityDefiner, V extends ObjectBodyData,
        T extends ObjectTypeDefiner<O, R, B, V, T, P>,
        P extends AbstractObjectProcessor<O, R, B, V, T, P>> {

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
    Class<V> getValueClass();

    /**
     * object processor
     */
    Class<P> getObjectProcessor();

}