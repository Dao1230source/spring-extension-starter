package org.source.spring.doc.object.entity;

import jakarta.validation.constraints.NotNull;
import org.source.spring.object.entity.ObjectBodyEntityDefiner;

public interface DocEntityDefiner extends ObjectBodyEntityDefiner {

    /**
     * fullName
     * <br>
     * 默认父级：root
     */
    @NotNull
    String getParentName();

    void setParentName(String parentName);
}