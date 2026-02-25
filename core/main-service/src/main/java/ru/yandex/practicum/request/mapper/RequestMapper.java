package ru.yandex.practicum.request.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import ru.yandex.practicum.request.dto.ParticipationRequestDto;
import ru.yandex.practicum.request.model.ParticipationRequest;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    RequestMapper INSTANCE = Mappers.getMapper(RequestMapper.class);

    @Mapping(target = "event", source = "event.id")
    @Mapping(target = "requester", source = "requester.id")
    ParticipationRequestDto toDto(ParticipationRequest request);

    List<ParticipationRequestDto> toDto(List<ParticipationRequest> requests);
}
