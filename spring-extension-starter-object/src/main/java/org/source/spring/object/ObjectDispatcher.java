package org.source.spring.object;

import org.source.spring.object.definer.entity.BodyDefiner;
import org.source.spring.object.definer.entity.ObjectDefiner;
import org.source.spring.object.definer.entity.RelationDefiner;
import org.source.spring.object.definer.enums.ObjectTypeDefiner;
import org.source.spring.object.definer.handler.ObjectTypeHandler;
import org.source.spring.object.definer.processor.BodyProcessor;
import org.source.utility.enums.BaseExceptionEnum;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class ObjectDispatcher {

    private static final Map<Integer, ObjectTypeDefiner<? extends BodyData>> TYPE_MAP = HashMap.newHashMap(64);
    private static final Map<Class<? extends BodyData>, ObjectTypeDefiner<? extends BodyData>> CLASS_MAP = HashMap.newHashMap(64);
    private static final Map<Class<? extends BodyProcessor<? extends ObjectDefiner, ? extends BodyDefiner, ? extends RelationDefiner, ? extends BodyData, ? extends ObjectTypeDefiner<? extends BodyData>>>,
            BodyProcessor<? extends ObjectDefiner, ? extends BodyDefiner, ? extends RelationDefiner, ? extends BodyData, ? extends ObjectTypeDefiner<? extends BodyData>>> PROCESS_CLASS_MAP = HashMap.newHashMap(64);

    public static synchronized <O extends ObjectDefiner, B extends BodyDefiner, R extends RelationDefiner,
            D extends BodyData, T extends ObjectTypeDefiner<D>, P extends BodyProcessor<O, B, R, D, T>> void register(
            ObjectTypeHandler<D, T> objectTypeHandler) {
        List<T> objectTypes = objectTypeHandler.allObjectTypes();
        objectTypes.forEach(t -> {
            TYPE_MAP.compute(t.getType(), (k, ov) -> {
                BaseExceptionEnum.IS_NULL.isNull(ov, "object type has exists, type:{}, desc:{}", k, t.getDesc());
                return t;
            });
            CLASS_MAP.compute(t.getDataClass(), (k, ov) -> {
                BaseExceptionEnum.IS_NULL.isNull(ov, "object type has exists, valueClass:{}, desc:{}", k, t.getDesc());
                return t;
            });
            Class<P> processorClass = t.getBodyProcessorClass();
            PROCESS_CLASS_MAP.computeIfAbsent(processorClass, k -> {
                P processor = objectTypeHandler.getSingleProcessor(processorClass);
                BaseExceptionEnum.NOT_NULL.nonNull(processor, "processor single object get error, processClass:{}", k);
                return processor;
            });
        });
    }

    public static <D extends BodyData, T extends ObjectTypeDefiner<D>> T getByType(Integer objectType) {
        ObjectTypeDefiner<? extends BodyData> objectTypeDefiner = TYPE_MAP.get(objectType);
        BaseExceptionEnum.NOT_NULL.nonNull(objectTypeDefiner, "objectTypeDefiner not found, objectType:{}", objectType);
        return (T) objectTypeDefiner;
    }

    public static <D extends BodyData, T extends ObjectTypeDefiner<D>> T getByClass(Class<D> cls) {
        ObjectTypeDefiner<? extends BodyData> objectTypeDefiner = CLASS_MAP.get(cls);
        BaseExceptionEnum.NOT_NULL.nonNull(objectTypeDefiner, "objectTypeDefiner not found, objectBodyData class:{}", cls);
        return (T) objectTypeDefiner;
    }

    public static <O extends ObjectDefiner, B extends BodyDefiner, R extends RelationDefiner,
            D extends BodyData, T extends ObjectTypeDefiner<D>, P extends BodyProcessor<O, B, R, D, T>> P getProcessorByType(Integer objectType) {
        T objectTypeDefiner = getByType(objectType);
        BodyProcessor<?, ?, ?, ?, ?> objectProcessor = PROCESS_CLASS_MAP.get(objectTypeDefiner.getBodyProcessorClass());
        BaseExceptionEnum.NOT_NULL.nonNull(objectProcessor, "objectProcessor not found, objectType:{}", objectType);
        return (P) objectProcessor;
    }

}
