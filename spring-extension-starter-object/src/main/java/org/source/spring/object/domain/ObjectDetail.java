package org.source.spring.object.domain;

import lombok.Builder;
import lombok.Data;
import org.jspecify.annotations.Nullable;
import org.source.spring.object.BodyData;
import org.source.spring.object.ObjectDispatcher;
import org.source.spring.object.ObjectElement;
import org.source.spring.object.definer.entity.ObjectDefiner;
import org.source.spring.object.definer.entity.RelationDefiner;
import org.source.spring.object.definer.enums.ObjectTypeDefiner;

import java.util.Collection;
import java.util.Objects;

@Builder
@Data
public class ObjectDetail<
        O extends ObjectDefiner,
        R extends RelationDefiner,
        D extends BodyData> {

    private String objectId;
    private @Nullable O object;
    private @Nullable R relation;
    private @Nullable D data;
    /*
    以下是辅助数据
     */
    private @Nullable Collection<R> byObjectIdRelations;
    private @Nullable Collection<R> byParentObjectIdRelations;

    public ObjectElement<D> toObjectElement() {
        if (Objects.isNull(this.object)) {
            return null;
        }
        ObjectElement<D> objectElement = new ObjectElement<>();
        if (Objects.nonNull(object)) {
            ObjectTypeDefiner<D> objectType = ObjectDispatcher.getByType(object.getType());
            objectElement.setType(objectType.getType());
        }
        if (Objects.nonNull(data)) {
            if (Objects.nonNull(relation)) {
                data.setParentObjectId(relation.getParentObjectId());
                data.setRelationType(relation.getType());
                data.setSorted(relation.getSorted());
            }
            objectElement.setData(data);
        }
        return objectElement;
    }
}
