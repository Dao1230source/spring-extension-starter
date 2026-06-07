package org.source.spring.object;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.source.spring.object.enums.StatusEnum;
import org.source.utility.tree.EnhanceNode;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Setter
@Getter
public class ObjectNode<V extends ObjectBodyData> extends EnhanceNode<String, ObjectElement<V>, ObjectNode<V>> {
    private @Nullable StatusEnum status;

    /**
     * 该节点和父节点的关联关系类型映射
     * {@literal <id, type>}
     */
    private Map<String, Integer> parentIdRelationTypeMap = new ConcurrentHashMap<>();

    @Override
    public ObjectNode<V> emptyNode() {
        return new ObjectNode<>();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ObjectNode<?> that = (ObjectNode<?>) o;
        return super.equals(o) && getStatus() == that.getStatus();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getStatus());
    }

    @Override
    public String toString() {
        return "status: " + this.getStatus() +
                ", element: " + super.toString();
    }
}