package org.source.spring.object;

import java.io.Serializable;

public interface Value extends Serializable {
    /**
     * 对象ID，唯一
     */
    String getObjectId();

    void setObjectId(String objectId);
}
