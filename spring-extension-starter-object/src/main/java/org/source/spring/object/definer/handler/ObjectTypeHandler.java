package org.source.spring.object.definer.handler;

import org.source.spring.object.BodyData;
import org.source.spring.object.definer.entity.BodyDefiner;
import org.source.spring.object.definer.entity.ObjectDefiner;
import org.source.spring.object.definer.entity.RelationDefiner;
import org.source.spring.object.definer.enums.ObjectTypeDefiner;
import org.source.spring.object.definer.processor.BodyProcessor;

import java.util.List;

public interface ObjectTypeHandler<D extends BodyData, T extends ObjectTypeDefiner<D>> {

    List<T> allObjectTypes();

    <O extends ObjectDefiner, B extends BodyDefiner, R extends RelationDefiner, P extends BodyProcessor<O, B, R, D, T>>
    P getSingleProcessor(Class<P> pClass);
}
