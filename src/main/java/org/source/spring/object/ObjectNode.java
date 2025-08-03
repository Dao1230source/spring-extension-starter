package org.source.spring.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.source.utility.tree.EnhanceNode;
import org.source.utility.tree.define.EnhanceElement;

@Setter
@Getter
@EqualsAndHashCode(callSuper = true, exclude = {"oldElement", "oldStatus"})
public class ObjectNode<I extends Comparable<I>, E extends EnhanceElement<I>> extends EnhanceNode<I, E, ObjectNode<I, E>> {
    private StatusEnum status;
    @JsonIgnore
    private E oldElement;
    @JsonIgnore
    private StatusEnum oldStatus;

    @Override
    public ObjectNode<I, E> emptyNode() {
        return new ObjectNode<>();
    }
}