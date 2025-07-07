package org.source.spring.doc.object.handler;

import org.source.spring.doc.data.DocData;
import org.source.spring.doc.object.entity.DocEntityDefiner;
import org.source.spring.doc.object.enums.DocObjectTypeEnum;
import org.source.spring.object.AbstractObjectProcessor;
import org.source.spring.object.AbstractValue;
import org.source.spring.object.entity.ObjectBodyEntityDefiner;
import org.source.spring.object.entity.ObjectEntityDefiner;
import org.source.spring.object.entity.RelationEntityDefiner;
import org.source.spring.object.enums.ObjectTypeDefiner;
import org.source.spring.object.handler.AbstractObjectTypeHandler;
import org.source.spring.utility.SpringUtil;
import org.source.utility.utils.Enums;
import org.springframework.boot.autoconfigure.AutoConfiguration;

import java.util.Map;

@AutoConfiguration
public class DocObjectTypeHandler<B extends DocEntityDefiner> extends AbstractObjectTypeHandler<B, DocData, DocObjectTypeEnum> {
    @SuppressWarnings("unchecked")
    @Override
    public Map<Integer, AbstractObjectProcessor<ObjectEntityDefiner, RelationEntityDefiner,
            ObjectBodyEntityDefiner, AbstractValue, ObjectTypeDefiner, Object>> objectType2ProcessorMap() {
        return Enums.toMap(DocObjectTypeEnum.class, DocObjectTypeEnum::getType, e -> SpringUtil.getBean(e.getObjectProcessor()));
    }

    @Override
    public Map<Integer, DocObjectTypeEnum> type2ObjectTypeMap() {
        return Enums.toMap(DocObjectTypeEnum.class, DocObjectTypeEnum::getType);
    }

    @Override
    public Map<Class<? extends DocData>, DocObjectTypeEnum> class2ObjectTypeMap() {
        return Enums.toMap(DocObjectTypeEnum.class, DocObjectTypeEnum::getValueClass);
    }
}
