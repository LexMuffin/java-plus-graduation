package ru.yandex.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import ru.yandex.practicum.EndpointHitDto;
import ru.yandex.practicum.model.EndpointHit;

@Mapper(componentModel = "spring")
public interface StatsMapper {

    StatsMapper INSTANCE = Mappers.getMapper(StatsMapper.class);

    @Mapping(target = "id", ignore = true)
    EndpointHit toEntity(EndpointHitDto dto);

    EndpointHitDto toDto(EndpointHit entity);
}
