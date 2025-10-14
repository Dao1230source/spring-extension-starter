package org.source.spring.object.handler;

import jakarta.validation.constraints.NotEmpty;
import org.source.spring.object.entity.RelationEntityDefiner;

import java.util.Collection;
import java.util.List;

public interface RelationDbHandlerDefiner<R extends RelationEntityDefiner> {
    /**
     * relation
     */
    R newRelationEntity();

    List<R> findRelationsByObjectIds(@NotEmpty Collection<String> objectIds);

    List<R> findRelationsByParentObjectIds(@NotEmpty Collection<String> parentObjectIds);

    List<R> findRelationsByBelongIds(@NotEmpty Collection<String> belongIds);

    void saveRelations(@NotEmpty Collection<R> relations);

    void removeRelations(@NotEmpty Collection<String> objectIds);

}