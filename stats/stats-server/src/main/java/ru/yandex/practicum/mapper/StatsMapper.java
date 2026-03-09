package ru.yandex.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.dto.stats.EndpointHitDto;
import ru.yandex.practicum.model.EndpointHit;

@Mapper(componentModel = "spring")
public interface StatsMapper {

    EndpointHitDto toDto(EndpointHit entity);

    @Mapping(target = "id", ignore = true)
    EndpointHit toEntity(EndpointHitDto dto);
}
