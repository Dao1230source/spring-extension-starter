package org.source.spring.object.definer.enums;

import org.source.spring.object.ObjectBodyData;
import org.source.spring.object.definer.entity.ObjectBodyEntityDefiner;
import org.source.spring.object.definer.entity.ObjectEntityDefiner;
import org.source.spring.object.definer.entity.RelationEntityDefiner;
import org.source.spring.object.definer.processor.AbstractObjectProcessor;

public interface ObjectTypeDefiner<D extends ObjectBodyData> {

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
     * 必须要有无参构造器
     */
    Class<? extends D> getValueClass();

    /**
     * object processor
     */
    <O extends ObjectEntityDefiner, B extends ObjectBodyEntityDefiner, R extends RelationEntityDefiner,
            T extends ObjectTypeDefiner<D>, P extends AbstractObjectProcessor<O, B, R, D, T>>
    Class<P> getObjectProcessor();

}