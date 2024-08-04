package org.source.spring.log;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface LogDataMapper {
    LogDataMapper INSTANCE = Mappers.getMapper(LogDataMapper.class);

    LogData copy(LogData logData);
}
