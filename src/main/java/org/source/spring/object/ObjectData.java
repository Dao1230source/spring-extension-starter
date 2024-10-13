package org.source.spring.object;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.source.utility.tree.identity.StringElement;

@EqualsAndHashCode(callSuper = false)
@Data
public class ObjectData extends StringElement {

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
    private ValueData value;
    /**
     * 类型
     */
    private Integer type;

    /**
     * 新增对象
     */
    private boolean newObject;

    @Override
    public @NonNull String getId() {
        return objectId;
    }

    @Override
    public String getParentId() {
        return parentObjectId;
    }
}
