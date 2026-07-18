package org.source.spring.object.definer.processor;

import org.source.spring.object.ObjectBodyData;
import org.source.spring.object.ObjectNode;
import org.source.spring.object.definer.entity.ObjectBodyDefiner;
import org.source.spring.object.definer.entity.ObjectDefiner;
import org.source.spring.object.definer.entity.RelationDefiner;
import org.source.spring.object.definer.enums.ObjectTypeDefiner;
import org.source.spring.object.definer.handler.*;

import java.util.Collection;

public interface ObjectProcessor<O extends ObjectDefiner,
        B extends ObjectBodyDefiner,
        R extends RelationDefiner,
        D extends ObjectBodyData,
        T extends ObjectTypeDefiner<D>> {

    ObjectHandler<O> getObjectHandler();

    ObjectBodyHandler<B> getObjectBodyHandler();

    RelationHandler<R> getRelationHandler();

    ObjectTypeHandler<D, T> getObjectTypeHandler();

    ObjectUidHandler getObjectUidHandler();

    void save(Collection<D> ds);

    void delete(Collection<String> objectIds);

    void remove(Collection<String> objectIds);

    ObjectNode<D> find(Collection<String> objectIds);
}
