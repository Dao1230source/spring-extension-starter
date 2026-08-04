package org.source.spring.object.definer.handler;

import org.source.spring.object.definer.entity.RelationDefiner;
import org.source.spring.object.domain.RelationUniqueKey;

import java.util.Collection;
import java.util.List;

public interface RelationHandler<R extends RelationDefiner> {
    /**
     * relation
     */
    R newRelation();

    List<R> findByObjectIds(Collection<String> objectIds);

    List<R> findByParentObjectIds(Collection<String> parentObjectIds);

    void saveRelations(Collection<R> relations);

    void removeByObjectIds(Collection<String> objectIds);

    void removeByParentObjectIds(Collection<String> objectIds);

    List<R> findByRelateUniqueKeys(Collection<RelationUniqueKey> relationUniqueKeys);

    void removeByRelationIds(Collection<Long> relationIds);
}