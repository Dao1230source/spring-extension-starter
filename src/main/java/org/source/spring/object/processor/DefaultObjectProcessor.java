package org.source.spring.object.processor;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.source.spring.object.AbstractValue;
import org.source.spring.object.entity.ObjectEntity;
import org.source.spring.object.entity.RelationEntity;

@Getter
public class DefaultObjectProcessor extends AbstractObjectProcessor<ObjectEntity, RelationEntity, AbstractValue> {

    @Override
    public @NotNull ObjectEntity newObjectEntity() {
        return new ObjectEntity();
    }

    @Override
    public @NotNull RelationEntity newRelationEntity() {
        return new RelationEntity();
    }
}
