package org.source.spring.object.definer.handler;

import org.source.spring.common.exception.SpExtExceptionEnum;
import org.source.spring.object.JsonUtil;
import org.source.spring.object.ObjectBodyData;
import org.source.spring.object.ObjectElement;
import org.source.spring.object.definer.entity.ObjectBodyEntityDefiner;
import org.source.spring.object.definer.entity.ObjectEntityDefiner;
import org.source.spring.object.definer.entity.RelationEntityDefiner;
import org.source.spring.object.definer.enums.ObjectTypeDefiner;
import org.source.spring.object.definer.processor.AbstractObjectProcessor;
import org.source.utility.assign.Assign;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ObjectTypeHandlerDefiner<O extends ObjectEntityDefiner, R extends RelationEntityDefiner,
        B extends ObjectBodyEntityDefiner, D extends ObjectBodyData,
        T extends ObjectTypeDefiner<O, R, B, D, T, P>,
        P extends AbstractObjectProcessor<O, R, B, D, T, P>> {

    Map<Integer, T> typeMap();

    Map<Class<D>, T> classTypeMap();

    Map<Integer, P> typeProcessorMap();

    Map<Integer, Function<Collection<ObjectElement<D>>, Assign<ObjectElement<D>>>> typeAssignerMap();

    Map<Integer, Consumer<Collection<O>>> typeObjectOperateMap();

    /**
     * obtain type for object value
     */
    default T getObjectType(D d) {
        T type = this.classTypeMap().get(d.getClass());
        if (Objects.isNull(type)) {
            throw SpExtExceptionEnum.OBJECT_VALUE_CLASS_NOT_DEFINED.newException("class:{}", d.getClass());
        }
        return type;
    }

    /**
     * convert entity to value
     */
    default T getObjectType(Integer type) {
        T objectType = this.typeMap().get(type);
        if (Objects.isNull(objectType)) {
            throw SpExtExceptionEnum.OBJECT_TYPE_NOT_DEFINED.newException("type:{}", type);
        }
        return objectType;
    }

    default D convertToData(T objectType, B objectBodyEntity) {
        return JsonUtil.obj(objectBodyEntity.getValue(), objectType.getValueClass());
    }

}
