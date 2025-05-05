package org.source.spring.object.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.source.spring.object.AbstractValue;
import org.source.spring.object.data.ObjectFullData;

@Mapper
public interface ObjectFullDataMapper {

    ObjectFullDataMapper INSTANCE = Mappers.getMapper(ObjectFullDataMapper.class);

    ObjectFullData<AbstractValue> copy(ObjectFullData<AbstractValue> source);
}
