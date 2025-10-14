package org.source.spring.object;

import lombok.Getter;
import lombok.Setter;
import org.source.utility.tree.EnhanceNode;
import org.source.utility.utils.Jsons;

import java.util.List;
import java.util.Objects;

@Setter
@Getter
public class ObjectNode<V extends AbstractValue> extends EnhanceNode<String, ObjectElement<V>, ObjectNode<V>> {
    private StatusEnum status;

    private List<Integer> relationTypes;

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
                ", element: " + Jsons.str(this.getElement());
    }
}