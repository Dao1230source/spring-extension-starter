package org.source.spring.object.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.source.spring.object.ObjectStatusEnum;
import org.source.spring.object.ObjectValueElement;
import org.source.utility.tree.identity.StringElement;

import java.util.Objects;

@EqualsAndHashCode(callSuper = false)
@Data
public class ObjectData<V extends ObjectValueElement> extends StringElement {

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

    public ObjectStatusEnum obtainObjectStatus() {
        if (Objects.isNull(value)) {
            return null;
        }
        return value.getObjectStatus();
    }

    @Override
    public @NonNull String getId() {
        return objectId;
    }

    @Override
    public String getParentId() {
        return parentObjectId;
    }
}
