package org.source.spring.object.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.source.spring.object.AbstractValue;
import org.source.utility.tree.identity.StringElement;

@EqualsAndHashCode(callSuper = false)
@Data
public class ObjectData<V extends AbstractValue> extends StringElement {

    /**
     * 对象ID，唯一
     */
    private String objectId;
    /**
     * 父对象ID
     */
    private String parentObjectId;

    /**
     * 关键字
     */
    private String key;
    /**
     * 值
     */
    private V value;
    /**
     * 类型
     */
    private Integer type;

    @Override
    public @NonNull String getId() {
        return objectId;
    }

    @Override
    public String getParentId() {
        return parentObjectId;
    }
}
