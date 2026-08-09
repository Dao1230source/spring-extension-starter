package org.source.spring.object.domain;

import lombok.Data;
import org.source.spring.object.definer.entity.RelationDefiner;
import org.source.utility.constant.Constants;

/**
 * relation unique key
 */
@Data
public class RelationKey {

    private String objectId;
    private String parentObjectId;

    public static <R extends RelationDefiner> RelationKey of(R r) {
        RelationKey relationKey = new RelationKey();
        relationKey.setObjectId(r.getObjectId());
        relationKey.setParentObjectId(r.getParentObjectId());
        return relationKey;
    }

    public static <R extends RelationKey> RelationKey of(R r) {
        RelationKey relationKey = new RelationKey();
        relationKey.setObjectId(r.getObjectId());
        relationKey.setParentObjectId(r.getParentObjectId());
        return relationKey;
    }

    public RelationKey reversed() {
        RelationKey relationKey = new RelationKey();
        relationKey.setObjectId(this.parentObjectId);
        relationKey.setParentObjectId(this.objectId);
        return relationKey;
    }

    public String toPlainString() {
        return String.join(Constants.COLON, this.objectId, this.parentObjectId);
    }
}
