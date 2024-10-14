package org.source.spring.object;

import java.io.Serializable;

public interface ObjectValue extends Serializable {
    /**
     * 对象ID，唯一
     */
    String getObjectId();

    void setObjectId(String objectId);

    /**
     * 对象状态
     */
    ObjectStatusEnum getObjectStatus();

    void setObjectStatus(ObjectStatusEnum objectStatus);

}
