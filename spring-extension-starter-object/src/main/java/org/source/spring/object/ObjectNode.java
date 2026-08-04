package org.source.spring.object;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.source.spring.object.enums.StatusEnum;
import org.source.utility.tree.EnhanceNode;

import java.util.Objects;

@Setter
@Getter
public class ObjectNode<D extends BodyData> extends EnhanceNode<String, ObjectElement<D>, ObjectNode<D>> {
    private @Nullable StatusEnum status;

    @Override
    public ObjectNode<D> emptyNode() {
        return new ObjectNode<>();
    }

    @Override
    public boolean equals(@Nullable Object o) {
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