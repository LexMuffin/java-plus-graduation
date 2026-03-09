package ru.yandex.practicum.event.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.yandex.practicum.dto.category.CategoryDto;
import ru.yandex.practicum.dto.event.EventFullDto;
import ru.yandex.practicum.dto.event.EventShortDto;
import ru.yandex.practicum.dto.event.LocationDto;
import ru.yandex.practicum.dto.event.NewEventDto;
import ru.yandex.practicum.dto.user.UserShortDto;
import ru.yandex.practicum.event.model.Event;
import ru.yandex.practicum.event.model.Location;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "confirmedRequests", constant = "0L")
    @Mapping(target = "createdOn", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "state", constant = "PENDING")
    @Mapping(target = "views", constant = "0L")
    @Mapping(target = "category", source = "category")
    Event toEntity(NewEventDto dto);

    @Mapping(target = "category", source = "event", qualifiedByName = "mapCategoryToDto")
    @Mapping(target = "initiator", source = "initiator", qualifiedByName = "mapLongToUserShortDto")
    EventFullDto toFullDto(Event event);

    List<EventFullDto> toFullDto(List<Event> events);

    @Mapping(target = "category", source = "event", qualifiedByName = "mapCategoryToDto")
    EventShortDto toShortDto(Event event);

    List<EventShortDto> toShortDto(List<Event> events);

    LocationDto toLocationDto(Location location);

    Location toLocation(LocationDto dto);

    @Named("mapCategoryToDto")
    default CategoryDto mapCategoryToDto(Event event) {
        if (event == null || event.getCategory() == null) return null;
        return CategoryDto.builder()
                .id(event.getCategory())
                .build();
    }

    @Named("mapLongToUserShortDto")
    default UserShortDto mapLongToUserShortDto(Long userId) {
        if (userId == null) return null;
        return UserShortDto.builder()
                .id(userId)
                .build();
    }
}