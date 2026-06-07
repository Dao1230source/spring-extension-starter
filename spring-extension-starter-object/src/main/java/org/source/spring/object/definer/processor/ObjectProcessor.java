package org.source.spring.object.definer.processor;

import org.source.spring.object.ObjectBodyData;
import org.source.spring.object.ObjectNode;

import java.util.Collection;

public interface ObjectProcessor<D extends ObjectBodyData> {

    void save(Collection<D> ds);

    void delete(Collection<String> objectIds);

    void remove(Collection<String> objectIds);

    ObjectNode<D> find(Collection<String> objectIds);

    String objectId();

    String spaceId();

    String userId();

}
