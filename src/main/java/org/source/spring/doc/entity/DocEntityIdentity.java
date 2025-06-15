package org.source.spring.doc.entity;

import jakarta.validation.constraints.NotNull;
import org.source.spring.object.entity.ObjectBodyEntityIdentity;

public interface DocEntityIdentity extends ObjectBodyEntityIdentity {

    /**
     * fullName
     * <br>
     * 默认父级：root
     */
    @NotNull
    String getParentName();

    void setParentName(String parentName);
}