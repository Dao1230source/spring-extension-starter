package org.source.spring.object.definer.handler;

import org.source.spring.common.utility.SpringUtil;
import org.source.spring.object.ObjectBodyData;
import org.source.spring.object.ObjectElement;
import org.source.spring.object.definer.entity.ObjectBodyEntityDefiner;
import org.source.spring.object.definer.entity.ObjectEntityDefiner;
import org.source.spring.object.definer.entity.RelationEntityDefiner;
import org.source.spring.object.definer.enums.ObjectTypeDefiner;
import org.source.spring.object.definer.processor.AbstractObjectProcessor;
import org.source.utility.assign.Assign;
import org.source.utility.utils.Streams;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractObjectTypeHandler<
        O extends ObjectEntityDefiner, R extends RelationEntityDefiner,
        B extends ObjectBodyEntityDefiner, V extends ObjectBodyData,
        T extends ObjectTypeDefiner<O, R, B, V, T, P>,
        P extends AbstractObjectProcessor<O, R, B, V, T, P>>
        implements ObjectTypeHandlerDefiner<O, R, B, V, T, P> {
    private final Map<Integer, P> typeProcessorMap = new ConcurrentHashMap<>();
    private final Map<Integer, Function<Collection<ObjectElement<V>>, Assign<ObjectElement<V>>>> typeAssignerMap = new ConcurrentHashMap<>();
    private final Map<Integer, Consumer<Collection<O>>> typeObjectOperateMap = new ConcurrentHashMap<>();
    private final Map<Integer, T> typeMap = new ConcurrentHashMap<>();
    private final Map<Class<V>, T> classTypeMap = new ConcurrentHashMap<>();

    protected abstract List<T> allObjectTypes();

    @Override
    public Map<Integer, P> typeProcessorMap() {
        if (typeProcessorMap.isEmpty()) {
            Streams.of(allObjectTypes()).forEach(t -> {
                Class<P> objectProcessor = t.getObjectProcessor();
                P processor = SpringUtil.getBean(objectProcessor);
                typeProcessorMap.put(t.getType(), processor);
            });
        }
        return typeProcessorMap;
    }

    @Override
    public Map<Integer, Function<Collection<ObjectElement<V>>, Assign<ObjectElement<V>>>> typeAssignerMap() {
        if (typeAssignerMap.isEmpty()) {
            this.typeProcessorMap().forEach((k, p) -> typeAssignerMap.put(k, es -> Assign.build(es)
                    .addAcquire(p.getObjectBodyDbHandler()::findObjectBodies, ObjectBodyEntityDefiner::getObjectId)
                    .throwException()
                    .addAction(ObjectElement::getId)
                    .addAssemble((e, t) -> {
                        T objectType = p.getObjectTypeHandler().getObjectType(e.getType());
                        e.setData(p.getObjectTypeHandler().convertToData(objectType, t));
                    }).backAcquire().backAssign()));
        }
        return typeAssignerMap;
    }

    @Override
    public Map<Integer, Consumer<Collection<O>>> typeObjectOperateMap() {
        if (typeObjectOperateMap.isEmpty()) {
            this.typeProcessorMap().forEach((k, processor) ->
                    typeObjectOperateMap.put(k, es -> {
                        Set<String> objectIds = Streams.map(es, ObjectEntityDefiner::getObjectId).collect(Collectors.toSet());
                        processor.getObjectDbHandler().removeObjects(objectIds);
                        processor.getRelationDbHandler().removeRelations(objectIds);
                        processor.getObjectBodyDbHandler().removeObjectBodies(objectIds);
                    }));
        }
        return typeObjectOperateMap;
    }

    @Override
    public Map<Integer, T> typeMap() {
        if (typeMap.isEmpty()) {
            this.allObjectTypes().forEach(t -> typeMap.put(t.getType(), t));
        }
        return typeMap;
    }

    @Override
    public Map<Class<V>, T> classTypeMap() {
        if (this.classTypeMap.isEmpty()) {
            this.allObjectTypes().forEach(t -> this.classTypeMap.put(t.getValueClass(), t));
        }
        return classTypeMap;
    }
}
