package ru.yandex.practicum.request.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.yandex.practicum.category.model.Category;
import ru.yandex.practicum.dto.event.EventFullDto;
import ru.yandex.practicum.dto.request.ParticipationRequestDto;
import ru.yandex.practicum.dto.user.UserDto;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.event.model.Event;
import ru.yandex.practicum.request.model.ParticipationRequest;
import ru.yandex.practicum.user.model.User;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    @Mapping(target = "event", source = "event")
    @Mapping(target = "requester", source = "requester")
    ParticipationRequestDto toDto(ParticipationRequest request);

    List<ParticipationRequestDto> toDto(List<ParticipationRequest> requests);

    @Mapping(target = "id", ignore = true)
    User toUser(UserDto userDto);

    @Mapping(target = "initiator", source = "initiator", qualifiedByName = "mapUserShortDtoToLong")
    @Mapping(target = "category", source = "category", qualifiedByName = "mapCategoryIdToCategory")
    @Mapping(target = "confirmedRequests", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "location", ignore = true)
    Event toEvent(EventFullDto eventDto);

    @Named("mapUserShortDtoToLong")
    default Long mapUserShortDtoToLong(UserShortDto userShortDto) {
        if (userShortDto == null) return null;
        return userShortDto.getId();
    }

    @Named("mapCategoryIdToCategory")
    default Category mapCategoryIdToCategory(ru.yandex.practicum.dto.category.CategoryDto categoryDto) {
        if (categoryDto == null) return null;
        return Category.builder()
                .id(categoryDto.getId())
                .name(categoryDto.getName())
                .build();
    }
}