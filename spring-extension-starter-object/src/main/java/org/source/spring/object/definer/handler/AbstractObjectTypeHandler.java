package org.source.spring.object.definer.handler;

import org.source.spring.object.ObjectBodyData;
import org.source.spring.object.ObjectElement;
import org.source.spring.object.definer.entity.ObjectBodyDefiner;
import org.source.spring.object.definer.enums.ObjectTypeDefiner;
import org.source.spring.object.definer.processor.ObjectProcessor;
import org.source.utility.assign.Assign;
import org.source.utility.enums.BaseExceptionEnum;
import org.source.utility.utils.Jsons;
import org.source.utility.utils.Streams;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbstractObjectTypeHandler<D extends ObjectBodyData, T extends ObjectTypeDefiner<D>>
        implements ObjectTypeHandler<D, T> {
    private final Map<Class<? extends ObjectProcessor<?, ?, ?, D, T>>, ObjectProcessor<?, ?, ?, D, T>> classProcessorMap = new ConcurrentHashMap<>();
    private final Map<Integer, ObjectProcessor<?, ?, ?, D, T>> typeProcessorMap = new ConcurrentHashMap<>();
    private final Map<Integer, Function<Collection<ObjectElement<D>>, Assign<ObjectElement<D>>>> typeAssignerMap = new ConcurrentHashMap<>();
    private final Map<Integer, Consumer<Collection<String>>> typeObjectRemoveMap = new ConcurrentHashMap<>();
    private final Map<Integer, T> typeMap = new ConcurrentHashMap<>();
    private final Map<Class<? extends D>, T> classTypeMap = new ConcurrentHashMap<>();
    private final Map<Integer, Constructor<? extends D>> typeNoArgConstructorMap = new ConcurrentHashMap<>();

    protected abstract List<T> allObjectTypes();

    protected abstract Map<Class<? extends ObjectProcessor<?, ?, ?, D, T>>, ObjectProcessor<?, ?, ?, D, T>> obtainClassProcessorMap();

    @Override
    public Map<Class<? extends ObjectProcessor<?, ?, ?, D, T>>, ObjectProcessor<?, ?, ?, D, T>> classProcessorMap() {
        if (classProcessorMap.isEmpty()) {
            Map<Class<? extends ObjectProcessor<?, ?, ?, D, T>>, ObjectProcessor<?, ?, ?, D, T>> classObjectProcessorMap = this.obtainClassProcessorMap();
            // 校验所有 type 对应的 processor class 都获取了
            this.allObjectTypes().forEach(t -> BaseExceptionEnum.NOT_EXISTS.isTrue(classObjectProcessorMap.containsKey(t.getObjectProcessorClass()),
                    "type:{}-{}-{}的processor未注册", t.getType(), t.getDesc(), t.getObjectProcessorClass().getSimpleName()));
            this.classProcessorMap.putAll(classObjectProcessorMap);
        }
        return classProcessorMap;
    }


    @SuppressWarnings("unchecked")
    @Override
    public Map<Integer, ObjectProcessor<?, ?, ?, D, T>> typeProcessorMap() {
        if (typeProcessorMap.isEmpty()) {
            Streams.of(allObjectTypes()).forEach(t -> typeProcessorMap.put(t.getType(), this.classProcessorMap().get(t.getObjectProcessorClass())));
        }
        return typeProcessorMap;
    }

    @Override
    public Map<Integer, Function<Collection<ObjectElement<D>>, Assign<ObjectElement<D>>>> typeAssignerMap() {
        if (typeAssignerMap.isEmpty()) {
            this.typeProcessorMap().forEach((k, p) -> typeAssignerMap.put(k, es -> Assign.build(es)
                    .addAcquire(p.getObjectBodyHandler()::findObjectBodies, ObjectBodyDefiner::getObjectId)
                    .throwException()
                    .addAction(ObjectElement::getId)
                    .addAssemble((e, t) -> {
                        T objectType = p.getObjectTypeHandler().getObjectType(e.getType());
                        D data = Jsons.obj(t.getValue(), objectType.getValueClass());
                        e.setData(data);
                        e.getData().setObjectId(t.getObjectId());
                    }).backAcquire().backAssign()));
        }
        return typeAssignerMap;
    }

    @Override
    public Map<Integer, Consumer<Collection<String>>> typeObjectRemoveMap() {
        if (typeObjectRemoveMap.isEmpty()) {
            this.typeProcessorMap().forEach((k, processor) ->
                    typeObjectRemoveMap.put(k, objectIds -> {
                        processor.getObjectHandler().removeObjects(objectIds);
                        processor.getRelationHandler().removeRelations(objectIds);
                        processor.getObjectBodyHandler().removeObjectBodies(objectIds);
                    }));
        }
        return typeObjectRemoveMap;
    }

    @Override
    public Map<Integer, T> typeMap() {
        if (typeMap.isEmpty()) {
            this.allObjectTypes().forEach(t -> typeMap.put(t.getType(), t));
        }
        return typeMap;
    }

    @Override
    public Map<Class<? extends D>, T> classTypeMap() {
        if (this.classTypeMap.isEmpty()) {
            this.allObjectTypes().forEach(t -> this.classTypeMap.put(t.getValueClass(), t));
        }
        return classTypeMap;
    }

    @Override
    public Map<Integer, Constructor<? extends D>> typeNoArgConstructorMap() {
        if (typeNoArgConstructorMap.isEmpty()) {
            this.allObjectTypes().forEach(t -> {
                Integer type = t.getType();
                T objectType = this.getObjectType(type);
                try {
                    Constructor<? extends D> constructor = objectType.getValueClass().getConstructor();
                    this.typeNoArgConstructorMap.put(type, constructor);
                } catch (NoSuchMethodException e) {
                    throw BaseExceptionEnum.OBJECT_MUST_HAVE_NO_ARGS_CONSTRUCTOR.newException("type:{}", type);
                }
            });
        }
        return typeNoArgConstructorMap;
    }
}
