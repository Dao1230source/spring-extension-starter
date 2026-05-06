package org.source.spring.object.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.source.spring.object.ObjectBodyData;
import org.source.spring.object.ObjectElement;

@Mapper
public interface ObjectElementMapper {

    ObjectElementMapper INSTANCE = Mappers.getMapper(ObjectElementMapper.class);

    ObjectElement<ObjectBodyData> copy(ObjectElement<ObjectBodyData> source);
}
