package org.source.spring.object.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.source.spring.object.AbstractValue;
import org.source.spring.object.ObjectElement;

@Mapper
public interface ObjectFullDataMapper {

    ObjectFullDataMapper INSTANCE = Mappers.getMapper(ObjectFullDataMapper.class);

    ObjectElement<AbstractValue> copy(ObjectElement<AbstractValue> source);
}
