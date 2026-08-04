package org.source.spring.object.definer.enums;

import org.source.spring.object.BodyData;
import org.source.spring.object.definer.entity.BodyDefiner;
import org.source.spring.object.definer.entity.ObjectDefiner;
import org.source.spring.object.definer.entity.RelationDefiner;
import org.source.spring.object.definer.processor.BodyProcessor;

public interface ObjectTypeDefiner<D extends BodyData> {

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
    Class<? extends D> getDataClass();

    /**
     * object processor
     */
    <O extends ObjectDefiner, B extends BodyDefiner, R extends RelationDefiner,
            T extends ObjectTypeDefiner<D>, P extends BodyProcessor<O, B, R, D, T>>
    Class<P> getBodyProcessorClass();

}