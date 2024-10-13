package org.source.spring.object;

public interface ValueData {
    /**
     * 对象ID，唯一
     */
    String getObjectId();

    void setObjectId(String objectId);

    /**
     * 新增对象
     */
    boolean isNewObject();

    void setNewObject(boolean newObject);

}
