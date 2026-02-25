package ru.yandex.practicum.event.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import ru.yandex.practicum.category.model.Category;
import ru.yandex.practicum.event.dto.EventFullDto;
import ru.yandex.practicum.event.dto.EventShortDto;
import ru.yandex.practicum.event.dto.LocationDto;
import ru.yandex.practicum.event.dto.NewEventDto;
import ru.yandex.practicum.event.model.Event;
import ru.yandex.practicum.event.model.Location;
import ru.yandex.practicum.user.dto.UserShortDto;
import ru.yandex.practicum.user.model.User;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventMapper {

    EventMapper INSTANCE = Mappers.getMapper(EventMapper.class);

    @Mapping(target = "confirmedRequests", constant = "0L")
    @Mapping(target = "createdOn", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "state", constant = "PENDING")
    @Mapping(target = "views", constant = "0L")
    Event toEntity(NewEventDto dto);

    EventFullDto toFullDto(Event event);

    List<EventFullDto> toFullDto(List<Event> events);

    EventShortDto toShortDto(Event event);

    List<EventShortDto> toShortDto(List<Event> events);

    LocationDto toLocationDto(Location location);

    Location toLocation(LocationDto dto);

    UserShortDto toUserShortDto(User user);

    @Named("mapCategoryToId")
    default Long mapCategoryToId(Category category) {
        return category != null ? category.getId() : null;
    }

    @Named("mapIdToCategory")
    default Category mapIdToCategory(Long id) {
        if (id == null) return null;
        return Category.builder().id(id).build();
    }
}
