package org.source.spring.object.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.source.spring.object.ObjectElement;
import org.source.spring.object.handler.ObjectBodyValueHandlerDefiner;

@Mapper
public interface ObjectElementMapper {

    ObjectElementMapper INSTANCE = Mappers.getMapper(ObjectElementMapper.class);

    ObjectElement<ObjectBodyValueHandlerDefiner> copy(ObjectElement<ObjectBodyValueHandlerDefiner> source);
}
