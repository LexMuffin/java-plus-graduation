package ru.yandex.practicum.event.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import ru.yandex.practicum.category.dto.CategoryDto;
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

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "confirmedRequests", constant = "0L")
    @Mapping(target = "createdOn", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "initiator", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "state", constant = "PENDING")
    @Mapping(target = "views", constant = "0L")
    @Mapping(target = "category", source = "category", qualifiedByName = "mapIdToCategory")
    Event toEntity(NewEventDto dto);

    @Mapping(target = "category", source = "category", qualifiedByName = "mapCategoryToDto")
    EventFullDto toFullDto(Event event);

    List<EventFullDto> toFullDto(List<Event> events);

    @Mapping(target = "category", source = "category", qualifiedByName = "mapCategoryToDto")
    EventShortDto toShortDto(Event event);

    List<EventShortDto> toShortDto(List<Event> events);

    LocationDto toLocationDto(Location location);

    Location toLocation(LocationDto dto);

    @Named("mapCategoryToDto")
    default CategoryDto mapCategoryToDto(Category category) {
        if (category == null) return null;
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

    @Named("mapIdToCategory")
    default Category mapIdToCategory(Long id) {
        if (id == null) return null;
        return Category.builder().id(id).build();
    }

    default UserShortDto toUserShortDto(User user) {
        if (user == null) return null;
        return UserShortDto.builder()
                .id(user.getId())
                .name(user.getName())
                .build();
    }
}