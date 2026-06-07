package org.source.spring.object.definer.handler;

import org.source.spring.object.ObjectBodyData;
import org.source.spring.object.ObjectElement;
import org.source.spring.object.definer.enums.ObjectExceptionEnum;
import org.source.spring.object.definer.enums.ObjectTypeDefiner;
import org.source.spring.object.definer.processor.AbstractObjectProcessor;
import org.source.utility.assign.Assign;
import org.source.utility.enums.BaseExceptionEnum;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ObjectTypeHandlerDefiner<D extends ObjectBodyData, T extends ObjectTypeDefiner<D>> {

    Map<Integer, T> typeMap();

    Map<Class<? extends D>, T> classTypeMap();

    <P extends AbstractObjectProcessor<?, ?, ?, D, T>> Map<Integer, P> typeProcessorMap();

    Map<Integer, Function<Collection<ObjectElement<D>>, Assign<ObjectElement<D>>>> typeAssignerMap();

    Map<Integer, Consumer<Collection<String>>> typeObjectRemoveMap();

    /**
     * @return 无参构造器
     */
    Map<Integer, Constructor<? extends D>> typeNoArgConstructorMap();

    /**
     * obtain type for object value
     */
    default T getObjectType(D d) {
        T type = this.classTypeMap().get(d.getClass());
        if (Objects.isNull(type)) {
            throw ObjectExceptionEnum.OBJECT_VALUE_CLASS_NOT_DEFINED.newException("class:{}", d.getClass());
        }
        return type;
    }

    /**
     * convert entity to value
     */
    default T getObjectType(Integer type) {
        T objectType = this.typeMap().get(type);
        if (Objects.isNull(objectType)) {
            throw ObjectExceptionEnum.OBJECT_TYPE_NOT_DEFINED.newException("type:{}", type);
        }
        return objectType;
    }

    default D getEmptyData(Integer type) {
        Constructor<? extends D> dConstructor = this.typeNoArgConstructorMap().get(type);
        try {
            return dConstructor.newInstance();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw BaseExceptionEnum.NO_ARGS_CONSTRUCTOR_NEW_INSTANCE_ERROR.newException("type:{}", type);
        }
    }

}
