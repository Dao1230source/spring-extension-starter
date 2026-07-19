package org.source.spring.object.domain;

import lombok.Data;
import org.source.spring.object.definer.entity.RelationDefiner;

/**
 * relation unique key
 */
@Data
public class RelationUniqueKey {

    private String objectId;
    private String parentObjectId;

    public static <R extends RelationDefiner> RelationUniqueKey of(R r) {
        RelationUniqueKey relationUniqueKey = new RelationUniqueKey();
        relationUniqueKey.setObjectId(r.getObjectId());
        relationUniqueKey.setParentObjectId(r.getParentObjectId());
        return relationUniqueKey;
    }

    public static RelationUniqueKey of(RelationUniqueKey r) {
        RelationUniqueKey relationUniqueKey = new RelationUniqueKey();
        relationUniqueKey.setObjectId(r.getObjectId());
        relationUniqueKey.setParentObjectId(r.getParentObjectId());
        return relationUniqueKey;
    }
}
