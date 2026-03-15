package org.source.spring.object.handler;

import org.source.spring.object.entity.RelationEntityDefiner;

import java.util.Collection;
import java.util.List;

public interface RelationDbHandlerDefiner<R extends RelationEntityDefiner> {
    /**
     * relation
     */
    R newRelationEntity();

    List<R> findRelationsByObjectIds(Collection<String> objectIds);

    List<R> findRelationsByParentObjectIds(Collection<String> parentObjectIds);

    List<R> findRelationsByBelongIds(Collection<String> belongIds);

    void saveRelations(Collection<R> relations);

    void removeRelations(Collection<String> objectIds);

}