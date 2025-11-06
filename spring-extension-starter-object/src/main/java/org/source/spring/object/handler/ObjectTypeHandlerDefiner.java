package org.source.spring.object.handler;

import org.source.spring.common.exception.SpExtExceptionEnum;
import org.source.spring.object.AbstractObjectProcessor;
import org.source.spring.object.AbstractValue;
import org.source.spring.object.ObjectElement;
import org.source.spring.object.entity.ObjectBodyEntityDefiner;
import org.source.spring.object.entity.ObjectEntityDefiner;
import org.source.spring.object.entity.RelationEntityDefiner;
import org.source.spring.object.enums.ObjectTypeDefiner;
import org.source.utility.assign.Assign;
import org.source.utility.utils.Jsons;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ObjectTypeHandlerDefiner<B extends ObjectBodyEntityDefiner, V extends AbstractValue, T extends ObjectTypeDefiner> {

    Map<Integer, AbstractObjectProcessor<ObjectEntityDefiner, RelationEntityDefiner,
            ObjectBodyEntityDefiner, AbstractValue, ObjectTypeDefiner, Object>> objectType2ProcessorMap();

    Map<Integer, T> type2ObjectTypeMap();

    Map<Class<? extends V>, T> class2ObjectTypeMap();

    Map<Integer, AbstractObjectProcessor<ObjectEntityDefiner, RelationEntityDefiner,
            ObjectBodyEntityDefiner, AbstractValue, ObjectTypeDefiner, Object>> allTypeProcessors();

    Map<Integer, Function<Collection<ObjectElement<AbstractValue>>, Assign<ObjectElement<AbstractValue>>>> allTypeAssigners();

    Map<Integer, Consumer<Collection<ObjectEntityDefiner>>> allTypeObjectConsumers();

    /**
     * obtain type for object value
     */
    default ObjectTypeDefiner getObjectType(V v) {
        ObjectTypeDefiner type = this.class2ObjectTypeMap().get(v.getClass());
        if (Objects.isNull(type)) {
            throw SpExtExceptionEnum.OBJECT_VALUE_CLASS_NOT_DEFINED.except("class:{}", v.getClass());
        }
        return type;
    }

    /**
     * convert entity to value
     */
    default ObjectTypeDefiner getObjectType(Integer type) {
        ObjectTypeDefiner objectType = this.type2ObjectTypeMap().get(type);
        if (Objects.isNull(objectType)) {
            throw SpExtExceptionEnum.OBJECT_TYPE_NOT_DEFINED.except("type:{}", type);
        }
        return objectType;
    }

    default V convert2Value(ObjectTypeDefiner objectType, ObjectBodyEntityDefiner objectBodyEntity) {
        return Jsons.obj(objectBodyEntity.getValue(), objectType.getValueClass());
    }

    default ObjectElement<V> convertToObjectElement(ObjectTypeDefiner objectType, B entity) {
        ObjectElement<V> fullData = new ObjectElement<>();
        fullData.setType(objectType.getType());
        fullData.setName(entity.getName());
        fullData.setObjectId(entity.getObjectId());
        V value = this.convert2Value(objectType, entity);
        fullData.setValue(value);
        return fullData;
    }

}
