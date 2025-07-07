package org.source.spring.object.handler;

import lombok.AllArgsConstructor;
import org.source.spring.object.AbstractObjectProcessor;
import org.source.spring.object.AbstractValue;
import org.source.spring.object.data.ObjectFullData;
import org.source.spring.object.entity.ObjectBodyEntityDefiner;
import org.source.spring.object.entity.ObjectEntityDefiner;
import org.source.spring.object.entity.RelationEntityDefiner;
import org.source.spring.object.enums.ObjectTypeDefiner;
import org.source.spring.utility.SpringUtil;
import org.source.utility.assign.Assign;
import org.source.utility.utils.Streams;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
public abstract class AbstractObjectTypeHandler<B extends ObjectBodyEntityDefiner, V extends AbstractValue, T extends ObjectTypeDefiner>
        implements ObjectTypeHandlerDefiner<B, V, T> {
    protected static final Map<Integer, AbstractObjectProcessor<ObjectEntityDefiner, RelationEntityDefiner,
            ObjectBodyEntityDefiner, AbstractValue, ObjectTypeDefiner, Object>> ALL_TYPE_PROCESSORS = new ConcurrentHashMap<>();
    protected static final Map<Integer, Function<Collection<ObjectFullData<AbstractValue>>, Assign<ObjectFullData<AbstractValue>>>> ALL_TYPE_ASSIGNERS = new ConcurrentHashMap<>();
    protected static final Map<Integer, Consumer<Collection<ObjectEntityDefiner>>> ALL_TYPE_OBJECT_CONSUMERS = new ConcurrentHashMap<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Map<Integer, AbstractObjectProcessor<ObjectEntityDefiner, RelationEntityDefiner,
            ObjectBodyEntityDefiner, AbstractValue, ObjectTypeDefiner, Object>> allTypeProcessors() {
        if (ALL_TYPE_PROCESSORS.isEmpty()) {
            Map<String, AbstractObjectProcessor> beansOfType = SpringUtil.getBeansOfType(AbstractObjectProcessor.class);
            beansOfType.forEach((k, processor) -> ALL_TYPE_PROCESSORS.putAll(processor.getObjectTypeHandler().objectType2ProcessorMap()));
        }
        return ALL_TYPE_PROCESSORS;
    }

    @Override
    public Map<Integer, Function<Collection<ObjectFullData<AbstractValue>>, Assign<ObjectFullData<AbstractValue>>>> allTypeAssigners() {
        if (ALL_TYPE_ASSIGNERS.isEmpty()) {
            this.allTypeProcessors().forEach((k, p) ->
                    ALL_TYPE_ASSIGNERS.put(k, es -> assignByType(es, p)));
        }
        return ALL_TYPE_ASSIGNERS;
    }

    public Assign<ObjectFullData<AbstractValue>> assignByType(Collection<ObjectFullData<AbstractValue>> es,
                                                              AbstractObjectProcessor<?, ?, ?, ?, ?, ?> processor) {
        return Assign.build(es)
                .addAcquire(processor.getObjectBodyDbHandler()::findObjectBodies, ObjectBodyEntityDefiner::getObjectId)
                .throwException()
                .addAction(ObjectFullData::getObjectId)
                .addAssemble((e, t) -> {
                    e.setName(t.getName());
                    e.setValue(processor.getObjectTypeHandler().convert2Value(processor.getObjectTypeHandler().getObjectType(e.getType()), t));
                }).backAcquire().backAssign();
    }

    @Override
    public Map<Integer, Consumer<Collection<ObjectEntityDefiner>>> allTypeObjectConsumers() {
        if (ALL_TYPE_OBJECT_CONSUMERS.isEmpty()) {
            this.allTypeProcessors().forEach((k, processor) ->
                    ALL_TYPE_OBJECT_CONSUMERS.put(k, es -> {
                        Set<String> objectIds = Streams.map(es, ObjectEntityDefiner::getObjectId).collect(Collectors.toSet());
                        processor.getObjectDbHandler().removeObjects(objectIds);
                        processor.getRelationDbHandler().removeRelations(objectIds);
                        processor.getObjectBodyDbHandler().removeObjectBodies(objectIds);
                    }));
        }
        return ALL_TYPE_OBJECT_CONSUMERS;
    }
}
